package scott.sort.test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import scott.sort.api.exception.SortException;
import scott.sort.api.exception.execution.persist.SortPersistException;
import scott.sort.api.persist.PersistRequest;
import scott.sort.server.jdbc.query.QueryResult;

import com.smartstream.mi.model.XMLSyntaxModel;
import com.smartstream.mi.query.QXMLSyntaxModel;

/**
 * Tests that entities get garbage collected when not used
 * @author scott
 *
 */
public class TestGarbageCollection extends TestBase {


    @Ignore //ignoring for speed
    @Test
    public void testHoldingQueryResultPreventsGC() throws Exception {

        serverEntityContext.setAllowGarbageCollection(true);

        insert100FullSyntaxes();

        serverEntityContext.clear();

        Thread.sleep(1000);

        QueryResult<XMLSyntaxModel> result = serverEntityContext.performQuery(new QXMLSyntaxModel());
        Collection<UUID> uuids = new LinkedList<>();
        for (XMLSyntaxModel syn: result.getList()) {
            uuids.add(syn.getEntity().getUuid());
        }

        assertEquals(200, uuids.size()); //100 syntaxes and their subsyntaxes were returned by the query.

        //we hold onto the result reference preventing GC

        waitForEntitiesToBeCollected(uuids, false);
    }

    @Test
    public void testLosingQueryResultCausesGC() throws Exception {

        serverEntityContext.setAllowGarbageCollection(true);

        insert100FullSyntaxes();

        serverEntityContext.clear();

        Thread.sleep(1000);

        QueryResult<XMLSyntaxModel> result = serverEntityContext.performQuery(new QXMLSyntaxModel());
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
        Iterator<XMLSyntaxModel> i = result.getList().iterator();
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
            XMLSyntaxModel syntax = TestPersistence.buildSyntax(serverEntityContext);
            request.save(syntax);
            if (i %  100 == 0) {
                serverEntityContext.persist(request);
                request = new PersistRequest();
            }
        }
        System.out.println("Time to save = " + (System.currentTimeMillis() - start) + " millis");
    }


}
