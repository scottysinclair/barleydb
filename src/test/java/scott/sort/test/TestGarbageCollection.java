package scott.sort.test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.Test;

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

    @Test
    public void testLosingQueryResultRefGarbageCollects() throws Exception {

        serverEntityContext.setAllowGarbageCollection(true);

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

        request = null;
        serverEntityContext.clear();

        Thread.sleep(1000);

        QueryResult<XMLSyntaxModel> result = serverEntityContext.performQuery(new QXMLSyntaxModel());
        Collection<UUID> uuids = new LinkedList<>();
        for (XMLSyntaxModel syn: result.getList()) {
            uuids.add(syn.getEntity().getUuid());
        }

        assertEquals(200, uuids.size()); //100 syntaxes and their subsyntaxes were returned by the query.

        int requiredCollected = uuids.size();
        result = null;
        System.gc();
        long until = System.currentTimeMillis() + (1000 * 10 * 100000);

        int countCollected = 0;
        while(System.currentTimeMillis() < until) {
            for (Iterator<UUID> i = uuids.iterator(); i.hasNext();) {
                if (serverEntityContext.getEntityByUuid(i.next(), false) == null) {
                    countCollected++;
                    i.remove();
                }
            }
            if (countCollected == requiredCollected) {
                System.out.println(countCollected + " collected in total");
                assertTrue(serverEntityContext.isCompletelyEmpty());
                return;
            }
            System.gc();
            Thread.sleep(500);
            System.out.println("FREE: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " KB");
        }
        fail("Entities not collected");
    }

}
