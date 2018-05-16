package scott.barleydb.xsd;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import static scott.barleydb.xsd.exception.MissingSchemaInfo.DependencyType.IMPORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.MissingSchemaInfo;
import scott.barleydb.xsd.exception.SchemaNotFoundException;
import scott.barleydb.xsd.exception.XsdDefinitionException;
import scott.barleydb.xsd.XsdAttribute;
import scott.barleydb.xsd.XsdDefinition;
import scott.barleydb.xsd.XsdElement;

public abstract class ParentTest {

    private static final Logger LOG = LoggerFactory.getLogger(ParentTest.class);


    public static XsdDefinition loadDefinition(String path) throws Exception {
        XsdDefinition xsdDefinition = new XsdDefinition(path, load(path), null, false);
        xsdDefinition.setDefinitionResolver(new TestXsdDefinitionResolver());
        xsdDefinition.resolveIncludesAndImports();
        /*
         * test that the resolver can be set to null after resolving
         * this is required for serializing across the wire.
         */
        xsdDefinition.setDefinitionResolver(null);
        return xsdDefinition;
    }

    private static void toOutputStream(InputStream in, OutputStream out) throws IOException {
        byte buf[] = new byte[1024];
        int len;
        try {
            while( (len = in.read(buf)) >= 0) {
                out.write(buf, 0, len);
                out.flush();
            }
            in.close();
        }
        catch(IOException x) {
            try {
                in.close();
            } catch (IOException y) {}
            throw x;
        }
    }

    public static byte[] load(String resource) throws IOException {
        InputStream in = XsdDefinition.class.getClassLoader().getResourceAsStream( resource );
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        toOutputStream(in, out);
        return out.toByteArray();
    }

    private static XsdDefinition resolveTestSchema(String fromSchemaLocation, XsdDefinition parent, String toSchemaLocation, MissingSchemaInfo.DependencyType dependencyType) throws SchemaNotFoundException, InvalidXsdException {
        int i = fromSchemaLocation.lastIndexOf('/');
        String path = i == -1 ? "" : fromSchemaLocation.substring(0, i);
        try {
            String fullPath = path + "/" + toSchemaLocation;
            LOG.info("Loading test schema at: " + fullPath);
            byte data[] = load(fullPath);
            if (data == null) {
                throw new SchemaNotFoundException(dependencyType, null, fullPath);
            }
            XsdDefinition xsdDefinition = new XsdDefinition(fullPath, data, parent, dependencyType == MissingSchemaInfo.DependencyType.INCLUDE);
            return xsdDefinition;
        }
        catch(SchemaNotFoundException x) {
            throw x;
        }
        catch(Exception x) {
            LOG.error("Caught exception resolving test schema", x);
            throw new InvalidXsdException("Unexpected error resolving test schema", x);
        }
    }

    protected static void assertMissingSchemaLocations(List<String> listofSchemaLocations, List<MissingSchemaInfo> schemaInfos) {
        assertCollections(listofSchemaLocations, new SchemInfoToSchemaLocation(), schemaInfos);
    }

    protected static void assertMissingNamespaceUris(List<String> listofNamespaceUris, List<MissingSchemaInfo> schemaInfos) {
        assertCollections(listofNamespaceUris, new SchemInfoToNamespaceUri(), schemaInfos);
    }

    protected static void assertElementNames(List<String> listOfElementNames, List<XsdElement> xsdElements) {
        assertCollections(listOfElementNames, new ElementToName(), xsdElements);
    }

    protected static void assertAttributeNames(List<String> listOfAttributeNames, List<XsdAttribute> xsdAttributes) {
        assertCollections(listOfAttributeNames, new AttributeToName(), xsdAttributes);
    }

    protected static void assertSchemaLocations(List<String> listOfSchemaLocations, Collection<XsdDefinition> xsdDefinitions) {
        assertCollections(listOfSchemaLocations, new XsdDefinitionToSchemaLocation(), xsdDefinitions);
    }

        /**
         * asserts that the mapped list of stuff is completely described by the list of expected things
         * @param expectedThings the list of things which we compare against
         * @param mapFunction the function used to generate a thing of type 'T' from some other stuff of type 'S'
         * @param listOfStuff the list of stuff we are going to convert and compare
         * @param <T>
         * @param <S>
         */
    protected static <T,S> void assertCollections(List<T> expectedThings, Map<S, T> mapFunction, Collection<S> listOfStuff) {
        assertEquals("incorrect number of elements in list", expectedThings.size(), listOfStuff.size());
        for (S stuff: listOfStuff)  {
            T thing = mapFunction.map(stuff);
            assertTrue("unexpected item " + thing, expectedThings.contains( thing ));
        }
    }


    interface Map<F,T> {
        public T map(F from);
    }

    static class SchemInfoToSchemaLocation implements Map<MissingSchemaInfo, String> {
        @Override
        public String map(MissingSchemaInfo from) {
            return from.getSchemaLocation();
        }
    }

    static class SchemInfoToNamespaceUri implements Map<MissingSchemaInfo, String> {
        @Override
        public String map(MissingSchemaInfo from) {
            return from.getNamespaceUri();
        }
    }

    static class ElementToName implements Map<XsdElement, String> {
        @Override
        public String map(XsdElement xsdElement) {
            try {
                return xsdElement.getElementName();
            }
            catch(XsdDefinitionException x) {
                throw new IllegalStateException("error getting element name", x);
            }
        }
    }
    static class AttributeToName implements Map<XsdAttribute, String> {
        @Override
        public String map(XsdAttribute xsdAttribute) {
            try {
                return xsdAttribute.getName();
            }
            catch(XsdDefinitionException x) {
                throw new IllegalStateException("error getting attribute name", x);
            }
        }
    }
    static class XsdDefinitionToSchemaLocation implements Map<XsdDefinition, String> {
        @Override
        public String map(XsdDefinition xsdDefinition) {
            return xsdDefinition.getSchemaLocation();
        }
    }

    public static class TestXsdDefinitionResolver implements XsdDefinition.DefinitionResolver {

        @Override
        public String getAbsoluteSchemaLocation(XsdDefinition forDefinition, String relativeSchemaLocation) {
            int i = forDefinition.getSchemaLocation().lastIndexOf('/');
            String path = i == -1 ? "" : forDefinition.getSchemaLocation().substring(0, i);
            return path + "/" + relativeSchemaLocation;
        }

        @Override
        public XsdDefinition resolveInclude(XsdDefinition forDef, String schemaLocation) throws SchemaNotFoundException, InvalidXsdException {
            return resolveTestSchema(forDef.getSchemaLocation(), forDef, schemaLocation, MissingSchemaInfo.DependencyType.INCLUDE);
        }

        @Override
        public Set<XsdDefinition> resolveImport(XsdDefinition forDef, String schemaLocation, String namespace) throws SchemaNotFoundException, InvalidXsdException {
            if (schemaLocation == null) {
                throw new SchemaNotFoundException(IMPORT, namespace, schemaLocation);
            }
            Set<XsdDefinition> result = new HashSet<>();
            XsdDefinition def = resolveTestSchema(forDef.getSchemaLocation(), forDef, schemaLocation, MissingSchemaInfo.DependencyType.IMPORT);
            if (def != null) {
                result.add(def);
            }
            return result;
        }

    }
}
