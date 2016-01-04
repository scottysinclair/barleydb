package scott.barleydb.test;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.example.mi.model.XmlSyntaxModel;
import org.example.mi.query.QXmlSyntaxModel;
import org.junit.Ignore;
import org.junit.Test;

import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.server.jdbc.query.QueryResult;

/**
 * Tests that entities get garbage collected when not used
 * @author scott
 *
 */
public class TestGarbageCollection extends TestBase {


    @Ignore //we shouldnt expect this to reliably work.
    @Test
    public void testHoldingQueryResultPreventsGC() throws Exception {

        serverEntityContext.setAllowGarbageCollection(true);

        insert100FullSyntaxes();

        serverEntityContext.clear();

        QueryResult<XmlSyntaxModel> result = serverEntityContext.performQuery(new QXmlSyntaxModel());
        Collection<UUID> uuids = new LinkedList<>();
        for (XmlSyntaxModel syn: result.getList()) {
            uuids.add(syn.getEntity().getUuid());
        }

        assertEquals(200, uuids.size()); //100 syntaxes and their subsyntaxes were returned by the query.

        //we hold onto the result reference preventing GC

        waitForEntitiesToBeCollected(uuids, false);
    }

    @Ignore
    @Test
    public void testLosingQueryResultCausesGC() throws Exception {

        serverEntityContext.setAllowGarbageCollection(true);

        insert100FullSyntaxes();

        serverEntityContext.clear();

        QueryResult<XmlSyntaxModel> result = serverEntityContext.performQuery(new QXmlSyntaxModel());
        Collection<UUID> uuids = new LinkedList<>();

        /*
         * If we use foreach with or without an iterator, then the iterator
         * stays on the stack and then when we go into the new stack frame for the call
         * waitForEntitiesToBeCollected the iterator will prevent the GC of the entities.
         *
         * Crazily enough, if you copy and paste the code from waitForEntitiesToBeCollected into this
         * method so that it executes within the same frame, then it GCs fine even with foreach.
         *
         */
        Iterator<XmlSyntaxModel> i = result.getList().iterator();
        while(i.hasNext()) {
            uuids.add(i.next().getEntity().getUuid());
        }
        i = null;


        assertEquals(200, uuids.size()); //100 syntaxes and their subsyntaxes were returned by the query.

        /*
         * Release the result reference allowing garbage collection.
         */
        result = null;

        waitForEntitiesToBeCollected(uuids, true);
    }

    @Ignore //we shouldnt expect this to reliably work.
    @Test
    public void testHoldingOnToProxyPreventsGC() throws Exception {
        serverEntityContext.setAllowGarbageCollection(true);

        insert100FullSyntaxes();

        serverEntityContext.clear();

        QueryResult<XmlSyntaxModel> result = serverEntityContext.performQuery(new QXmlSyntaxModel());
        Collection<UUID> uuids = new LinkedList<>();

        /*
         * We put the models into a normal list and hold it
         */
        List<XmlSyntaxModel> models = new ArrayList<>(result.getList());
        Iterator<XmlSyntaxModel> i = models.iterator();
        while(i.hasNext()) {
            uuids.add(i.next().getEntity().getUuid());
        }
        i = null;
        result = null;

        assertEquals(200, uuids.size()); //100 syntaxes and their subsyntaxes were returned by the query.

        waitForEntitiesToBeCollected(uuids, false);
    }

    private void waitForEntitiesToBeCollected(Collection<UUID> uuids, boolean expectedCollection) throws Exception {
        int requiredCollected = uuids.size();
        long until = System.currentTimeMillis() + (1000 * 3);
        int countCollected = 0;
        System.gc();
        while(System.currentTimeMillis() < until) {
            for (Iterator<UUID> i = uuids.iterator(); i.hasNext();) {
                if (serverEntityContext.getEntityByUuid(i.next(), false) == null) {
                    countCollected++;
                    i.remove();
                }
            }
            if (countCollected == requiredCollected) {
                if (!expectedCollection) {
                    fail("Did not expect GC, but it happened anyway");
                }
                System.out.println(countCollected + " collected in total");
                assertTrue(serverEntityContext.isCompletelyEmpty());
                return;
            }
            System.gc();
            Thread.sleep(500);
            System.out.println("FREE: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " KB");
        }
        if (expectedCollection) {
            fail("Entities not collected");
        }
    }

    private void insert100FullSyntaxes() throws SortException, SortPersistException {
        PersistRequest request = new PersistRequest();
        long start = System.currentTimeMillis();
        for (int i=1; i<=100; i++) {
            XmlSyntaxModel syntax = TestPersistence.buildSyntax(serverEntityContext);
            request.save(syntax);
            if (i %  100 == 0) {
                serverEntityContext.persist(request);
                request = new PersistRequest();
            }
        }
        System.out.println("Time to save = " + (System.currentTimeMillis() - start) + " millis");
    }


}
