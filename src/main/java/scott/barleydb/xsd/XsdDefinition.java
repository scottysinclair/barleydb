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

import static scott.barleydb.xsd.DomHelper.*;
import static scott.barleydb.xsd.SchemaElements.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.AttributeGroupNotFoundException;
import scott.barleydb.xsd.exception.AttributeNotFoundException;
import scott.barleydb.xsd.exception.ElementNotFoundException;
import scott.barleydb.xsd.exception.GroupNotFoundException;
import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.SchemaNotFoundException;
import scott.barleydb.xsd.exception.TypeNotFoundException;
import scott.barleydb.xsd.exception.XsdDefinitionException;
import scott.barleydb.xsd.exception.MissingSchemaInfo.DependencyType;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 *
 * Encapsulates the information in an Xsd document
 * providing an API for easy access to XSD structure
 *
 *
 *
 * Element declarations from a schema with no target namespace validate unqualified elements in the instance document.
 * That is, they validate elements for which no namespace qualification is provided by either an explicit prefix or by default (xmlns:).
 * So, to validate a traditional XML 1.0 document which does not use namespaces at all, you must provide a schema with no target namespace.
 * Of course, there are many XML 1.0 documents that do not use namespaces, so there will be many schema documents written without target namespaces;
 * you must be sure to give to your processor a schema document that corresponds to the vocabulary you wish to validate.
 *
 */
public class XsdDefinition implements XsdNode, Serializable {

    public interface DefinitionResolver {


        String getAbsoluteSchemaLocation(XsdDefinition forDefinition, String relativeSchemaLocation);
        /**
         * Resolves the include or throws an Exception
         * Subclass to specify how resolution is handled
         * @param forDefinition the definition which contains the include.
         * @param schemaLocation the schema location declared in the include
         * @return the resolved schema
         * @throws SchemaNotFoundException
         * @throws InvalidXsdException
         */
        XsdDefinition resolveInclude(XsdDefinition forDefinition, String schemaLocation) throws SchemaNotFoundException, InvalidXsdException;

        /**
         * resolves the import or throws an Exception
         * Subclass to specify how resolution is handled
         * @param forDefinition the definition which contains the import.
         * @param schemaLocation the relative location of the schema, can be null
         * @param namespace the namespace of the schema, can be null
         * @return the resolved schema
         * @throws SchemaNotFoundException if a specific schemaLocation was given but not found.
         * @throws InvalidXsdException
         */
        Set<XsdDefinition> resolveImport(XsdDefinition forDefinition, String schemaLocation, String namespace) throws SchemaNotFoundException, InvalidXsdException;
    }

    private static final long serialVersionUID = 1L;

    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    private static final Logger LOG = LoggerFactory.getLogger(XsdDefinition.class);

    private transient Document document;

    /**
     * Used to resolve definitions.
     */
    private DefinitionResolver definitionResolver = new DefaultDefinitionResolver();

    private String schemaLocation;


    /**
     * only set if this definition is used as a chameleon include
     */
    private String chameleonTargetNamespace;

    /**
     * Definitions which include us
     *
     */
    private Set<XsdDefinition> includesMe = new LinkedHashSet<>();

    /**
     * Definitions which import us
     */
    private Set<XsdDefinition> importsMe = new LinkedHashSet<>();

    /**
     * included definitions must all reference the same target namespace
     *
     */
    private Set<XsdDefinition> includes = new LinkedHashSet<>();

    /**
     * imported definitions
     */
    private Set<XsdDefinition> imports = new LinkedHashSet<>();

    /**
     * programmatically set content for XsdAny elements.
     */
    private transient Map<Node, XsdNode> xsdAnyContent = new IdentityHashMap<>();

    private boolean startedResolvedIncludesAndImports = false;

    public XsdDefinition(String schemaLocation, byte data[]) throws XsdDefinitionException {
        this(schemaLocation, data, null, false);
    }
    public XsdDefinition(String schemaLocation, byte data[], XsdDefinition parent, boolean wasIncluded) throws XsdDefinitionException {
        this.schemaLocation = schemaLocation;
        this.document = parse(data);
        if (parent != null) {
            if (wasIncluded) {
                includesMe.add(parent);
            }
            else {
                importsMe.add(parent);
            }
        }
        if (parent != null) {
            this.definitionResolver = parent.definitionResolver;
        }
        /*
         * If we are included, in a parent document
         * then verify that the include is ok or fail
         */
        if (wasIncluded) {
            String value = getAttribute(schemaElement(), "targetNamespace", null);
            if (value != null && !value.equals( parent.getTargetNamespace() )) {
                throw new XsdDefinitionException("Including an XSD with a different target namespace, my namespace=" + parent.getTargetNamespace() + "includes namespace=" + value);
            }
            if (value == null && parent != null) {
                chameleonTargetNamespace = parent.getTargetNamespace();
            }
        }

    }

    /**
     * We allow a doc to be reused when we have the same document being used in 2 different target namespaces as chamelion documents.
     * @param schemaLocation
     * @param doc
     * @param parent
     * @param wasIncluded
     * @param chameleonTargetNamespace
     * @throws XsdDefinitionException
     */
    private XsdDefinition(String schemaLocation, Document doc, XsdDefinition parent, boolean wasIncluded) throws XsdDefinitionException {
        this.schemaLocation = schemaLocation;
        this.document = doc;
        if (parent != null) {
            if (wasIncluded) {
                includesMe.add(parent);
            }
            else {
                importsMe.add(parent);
            }
        }
        if (parent != null) {
            this.definitionResolver = parent.definitionResolver;
        }
        /*
         * If we are included, in a parent document
         * then verify that the include is ok or fail
         */
        if (wasIncluded) {
            String value = getAttribute(schemaElement(), "targetNamespace", null);
            if (value != null && !value.equals( parent.getTargetNamespace() )) {
                throw new XsdDefinitionException("Including an XSD with a different target namespace, my namespace=" + parent.getTargetNamespace() + "includes namespace=" + value);
            }
            if (value == null && parent != null) {
                chameleonTargetNamespace = parent.getTargetNamespace();
            }
        }
    }


    /**
     * Can be changed at any time
     * Also changes the resolver of any included or imported documents
     * @param definitionResolver
     */
    public void setDefinitionResolver(DefinitionResolver definitionResolver) {
        setDefinitionResolver(definitionResolver, new HashSet<XsdDefinition>());
    }
    private void setDefinitionResolver(DefinitionResolver definitionResolver, Set<XsdDefinition> alreadyProcessed) {
        if (!alreadyProcessed.add(this)) {
            return;
        }
        this.definitionResolver = definitionResolver;
        for (XsdDefinition def: includes) {
            def.setDefinitionResolver(definitionResolver, alreadyProcessed);
        }
        for (XsdDefinition def: imports) {
            def.setDefinitionResolver(definitionResolver, alreadyProcessed);
        }
    }

    @Override
    public Node getDomNode() {
      return document;
    }

    @Override
    public XsdDefinition getXsdDefinition() {
        return this;
    }

    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    /**
     * The child target elements are the globally defined elements in the XSD
     * Including globally defined elements in any included XSDs.
     */
    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        List<XsdElement> result = new LinkedList<>();
        _getChildTargetElements(result, new HashSet<XsdDefinition>());
        return result;
    }

    private void _getChildTargetElements(List<XsdElement> result, Set<XsdDefinition> alreadyProcessed) throws XsdDefinitionException {
        if (!alreadyProcessed.add(this)) {
            return;
        }
        LOG.trace("{} getting child target elements {} {} " + System.identityHashCode(alreadyProcessed), getSchemaLocation(), System.identityHashCode(this));
        Element schema = getChildElement(document);
        for (Element childElement: getChildElements(schema, elementWithLocalName("element"))) {
            result.add(new XsdElement(this, childElement));
        }
        /*
         * Also check the includes for globally defined elements
         */
        for (XsdDefinition include: includes) {
//            LOG.debug("{} checking includes for globally defined elements", getSchemaLocation());
            include._getChildTargetElements(result, alreadyProcessed);
        }
    }

    /**
     * The mandatory schema location of this XsdDefinition
     * @return
     */
    public String getSchemaLocation() {
        return schemaLocation;
    }

    /**
     * The list of XsdDefinitions which are includes in this XsdDefinition
     * @return the list of definitions or the empty list if there are none.
     */
    public Set<XsdDefinition> getIncludes() {
        return Collections.unmodifiableSet( includes );
    }

    /**
     * The list of XsdDefinitions which are includes in this XsdDefinition
     * @return the list of definitions or the empty list if there are none
     */
    public Set<XsdDefinition> getImports() {
        return Collections.unmodifiableSet( imports );
    }

    public boolean isChamelionInclude() {
        return chameleonTargetNamespace != null;
    }

    /**
     * The target namespace of this XsdDefinition
     *
     * @return the target namespace or null if none is defined
     */
    public String getTargetNamespace() {
        if (chameleonTargetNamespace != null) {
            return chameleonTargetNamespace;
        }
        return getAttribute(schemaElement(), "targetNamespace", null);
    }

    /**
     * static helper method
     */
    public static String getTargetNamespace(Document doc) {
        return getAttribute(getChildElement(doc), "targetNamespace", null);
    }


    public String getDefaultNamespace() {
        return getAttribute(schemaElement(), "xmlns", null);
    }

    /**
     * If true then elements will belong to the target namespace by default.
     * otherwise each element will belong to the null namespace unless form="qualified" is specified on the element definition
     * @return
     */
    public boolean elementsAreQualifiedByDefault() {
        //if elementFormDefault is not defined, then it defaults to unqualified according to the xsd spec.
        return "qualified".equals( getAttribute(schemaElement(), "elementFormDefault", "unqualified") );
    }

    /**
     * If true then attributes will belong to the target namespace by default.
     * otherwise each attributes will belong to the null namespace unless form="qualified" is specified on the element definition
     * @return
     */
    public boolean attributesAreQualifiedByDefault() {
        //if attributeFormDefault is not defined, then it defaults to unqualified according to the xsd spec.
        return "qualified".equals( getAttribute(schemaElement(), "attributeFormDefault", "unqualified") );
    }

    /**
     * Resolves all imports and includes XsdDefinitions and links to them
     * @throws scott.barleydb.xsd.exception.SchemaNotFoundException
     * @throws scott.barleydb.xsd.exception.InvalidXsdException
     */
    public void resolveIncludesAndImports() throws SchemaNotFoundException, InvalidXsdException {
        if (startedResolvedIncludesAndImports) {
            LOG.debug("Already resolved imports for " + schemaLocation);
            return;
        }
        startedResolvedIncludesAndImports = true;
        SchemaNotFoundException schemaNotFoundException = null;
        for (Element element: getChildElements(schemaElement(), oneOf(elementWithLocalName("include"), elementWithLocalName("import")))) {
            switch (element.getLocalName()) {
                case "include":  {
                    validateInclude(element);
                    String schemaLocation = getAttribute(element, "schemaLocation", null);
                    try {
                        LOG.debug("Resolving include with schema location {}", schemaLocation);
                        XsdDefinition toInclude = findDefinitionForInclude(schemaLocation);
                        if (toInclude == null) {
                            toInclude = definitionResolver.resolveInclude(this, schemaLocation );
                            LOG.debug("Include with schema location {} is being added to the namespace {}", schemaLocation, getTargetNamespace());
                        }
                        else {
                            LOG.debug("Include with schema location {} is already loaded {}", schemaLocation, getTargetNamespace());
                            if (!Objects.equals(getTargetNamespace(), toInclude.getTargetNamespace())) {
                                /*
                                 * we found the matching include, but the target namespace does not match
                                 * perhaps it is a chamelion include in another namespace, in which case we should
                                 * reuse the DOM model wrapping it in a new XsdDefinition
                                 */
                                if (toInclude.isChamelionInclude()) {
                                    try {
                                        toInclude = new XsdDefinition(schemaLocation, document, this, true);
                                    }
                                    catch (XsdDefinitionException x) {
                                        throw new InvalidXsdException("Error reusing document inside new chamelion XsdDefinition", x);
                                    }

                                }
                                else {
                                    /*
                                     * the target namespace did not match and the document to include is not a chamelon include
                                     * seems that the include directive was stupidly trying to include a document from a different namespace.
                                     * The correct behaviour is to ignore the include.
                                     */
                                    toInclude = null;
                                }
                            }
                        }
                        if (toInclude != null) {
                            includes.add( toInclude );
                            toInclude.includesMe.add(this);
                            toInclude.resolveIncludesAndImports(); //noop if already done
                        }
                    }
                    catch(SchemaNotFoundException x) {
                        schemaNotFoundException = SchemaNotFoundException.merge(schemaNotFoundException, x);
                    }
                    break;
                }
                case "import":  {
                    final String namespace = getAttribute(element, "namespace", null);
                    final String schemaLocation = getAttribute(element, "schemaLocation", null);
                    LOG.debug("Resolving import with schema location {} and namespace", schemaLocation, namespace);
                    Set<XsdDefinition> toImport = new HashSet<>();
                    try {
                        if (schemaLocation != null) {
                            /*
                             * check if the definition is already loaded
                             */
                            XsdDefinition def = findDefinitionForImportWithLocation(schemaLocation);
                            if (def != null) {
                                /*
                                 * we found the definition already loaded, but it is a chamelion include
                                 * which means that it actually has the NO NAMESPACE namespace defined as the target definition
                                 * this means that "this" document is actually importing the NO NAMESPACE namespace to refer to
                                 * some of it's components.
                                 *
                                 */
                               if (def.isChamelionInclude()) {
                                   if (namespace == null) {
                                       String absoluteSchemaLocation = definitionResolver.getAbsoluteSchemaLocation(this, schemaLocation);
                                       try {
                                            def = new XsdDefinition(absoluteSchemaLocation, def.document, this, false);
                                        }
                                       catch (XsdDefinitionException x) {
                                           throw new InvalidXsdException("Could not create new XsdDefinition for import from chamelion document", x);
                                        }
                                   }
                                   else {
                                       throw new InvalidXsdException("Bad import: the schemaLocation " + schemaLocation + " points to a NO NAMESPACE schema, but the namespace is expected to be " + namespace);
                                   }
                               }
                               toImport.add( def );
                            }
                            else {
                                /*
                                 * the schema location it is not loaded, try and load it
                                 */
                                toImport.addAll( definitionResolver.resolveImport(this, schemaLocation, namespace) );

                            }
                        }
                        else {
                            try {
                                toImport.addAll( findDefinitionsForImportWithTargetNamespace(namespace) );
                            }
                            catch (XsdDefinitionException x) {
                                throw new InvalidXsdException("Could not create new XsdDefinition for import from chamelion document", x);
                            }
                            toImport.addAll( definitionResolver.resolveImport(this, schemaLocation , namespace) );
                        }
                    }
                    catch(SchemaNotFoundException x) {
                        schemaNotFoundException = SchemaNotFoundException.merge(schemaNotFoundException, x);
                    }
                    for (XsdDefinition def: toImport) {
                        if (!Objects.equals(namespace, def.getTargetNamespace())) {
                            throw new InvalidXsdException("The namespace attribute of <xsd:import> must match the target namespace of the imported document (or be missing when there is no target namespace) +" +
                                    " schemaLocation=" + schemaLocation + ", namespace=" + namespace);
                        }
                        imports.add(  def );
                        def.importsMe.add(this);
                        try {
                            def.resolveIncludesAndImports();
                        }
                        catch(SchemaNotFoundException x) {
                            schemaNotFoundException = SchemaNotFoundException.merge(schemaNotFoundException, x);
                        }
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("Element must be an include or an  import");
                }
            }
        }
        if (schemaNotFoundException != null) {
            throw schemaNotFoundException;
        }
    }

    void setXsdAnyContent(XsdAny xsdAny, XsdNode content) {
        xsdAnyContent.put(xsdAny.getDomElement(), content);
    }

    XsdNode getXsdAnyContent(XsdAny xsdAny) {
        return xsdAnyContent.get(xsdAny.getDomElement());
    }

    private XsdDefinition findDefinitionForInclude(String schemaLocation) {
        String absoluteSchemaLocation = definitionResolver.getAbsoluteSchemaLocation(this, schemaLocation);
        return _findDefinitionForInclude(absoluteSchemaLocation, new HashSet<XsdDefinition>());
    }
    /**
     * Checks the whole definitions graph tree for the given input
     * there can be cyclic references between includes so we track processing using the alreadyChecked set
     */
    private XsdDefinition _findDefinitionForInclude(String absoluteSchemaLocation, Set<XsdDefinition> alreadyChecked) {
        if (alreadyChecked.contains(this)) {
            return null;
        }
        alreadyChecked.add(this);
        if (getSchemaLocation().equals(absoluteSchemaLocation)) {
            /*
             * we are it
             */
            return this;
        }
        /*
         * check our includes
         */
        for (XsdDefinition include: includes) {
            XsdDefinition found = include._findDefinitionForInclude(absoluteSchemaLocation, alreadyChecked);
            if (found != null) {
                return found;
            }
        }
        /*
         * check documents which include us
         */
        for (XsdDefinition def: includesMe) {
            XsdDefinition found = def._findDefinitionForInclude(absoluteSchemaLocation, alreadyChecked);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Set<XsdDefinition> findDefinitionsForImportWithTargetNamespace(String targetNamespace) throws XsdDefinitionException {
        Set<XsdDefinition> result = new HashSet<XsdDefinition>();
        _findDefinitionsForImportWithTargetNamespace(targetNamespace, result, new HashSet<XsdDefinition>());
        return result;
    }

    /**
     * Checks the whole definitions graph tree for the given input
     * there can be cyclic references between includes so we track processing using the alreadyChecked set
     * @throws XsdDefinitionException
     */
    private void _findDefinitionsForImportWithTargetNamespace(String targetNamespace, Set<XsdDefinition> result, Set<XsdDefinition> alreadyChecked) throws XsdDefinitionException {
        if (alreadyChecked.contains(this)) {
            return;
        }
        alreadyChecked.add(this);
        if (isChamelionInclude()) {
            if (targetNamespace == null) {
                /*
                 * we are looking for imports for the NO_NAMESPACE namespace
                 * we are actually a Chamelion definition, meaning our XSD dom is has no target namespace
                 * we create a new XsdDefinition based on the same dom which
                 */
                result.add(new XsdDefinition(getSchemaLocation(), document, null, false));
            }
        }
        else if (Objects.equals(getTargetNamespace(), targetNamespace)) {
            result.add(this);
        }
        /*
         * check our includes
         */
        for (XsdDefinition include: includes) {
            include._findDefinitionsForImportWithTargetNamespace(targetNamespace, result, alreadyChecked);
        }
        /*
         * check our imports
         */
        for (XsdDefinition imp: imports) {
            imp._findDefinitionsForImportWithTargetNamespace(targetNamespace, result, alreadyChecked);
        }
        /*
         * check documents which include us
         */
        for (XsdDefinition def: includesMe) {
            def._findDefinitionsForImportWithTargetNamespace(targetNamespace, result, alreadyChecked);
        }
        /*
         * check documents which import us
         */
        for (XsdDefinition def: importsMe) {
            def._findDefinitionsForImportWithTargetNamespace(targetNamespace, result, alreadyChecked);
        }
    }

    private XsdDefinition findDefinitionForImportWithLocation(String schemaLocation) {
        String absoluteSchemaLocation = definitionResolver.getAbsoluteSchemaLocation(this, schemaLocation);
        return _findDefinitionForImportWithLocation(absoluteSchemaLocation, new HashSet<XsdDefinition>());
    }
    /**
     * Checks the whole definitions graph tree for the given input
     * there can be cyclic references between includes so we track processing using the alreadyChecked set
     */
    private XsdDefinition _findDefinitionForImportWithLocation(String absoluteSchemaLocation, Set<XsdDefinition> alreadyChecked) {
        if (alreadyChecked.contains(this)) {
            return null;
        }
        alreadyChecked.add(this);
        if (getSchemaLocation().equals(absoluteSchemaLocation)) {
            /*
             * we are it
             */
            return this;
        }
        /*
         * check our includes
         */
        for (XsdDefinition include: includes) {
            XsdDefinition found = include._findDefinitionForImportWithLocation(absoluteSchemaLocation, alreadyChecked);
            if (found != null) {
                return found;
            }
        }
        /*
         * check our imports
         */
        for (XsdDefinition imp: imports) {
            XsdDefinition found = imp._findDefinitionForImportWithLocation(absoluteSchemaLocation, alreadyChecked);
            if (found != null) {
                return found;
            }
        }
        /*
         * check documents which include us
         */
        for (XsdDefinition def: includesMe) {
            XsdDefinition found = def._findDefinitionForImportWithLocation(absoluteSchemaLocation, alreadyChecked);
            if (found != null) {
                return found;
            }
        }
        /*
         * check documents which import us
         */
        for (XsdDefinition def: importsMe) {
            XsdDefinition found = def._findDefinitionForImportWithLocation(absoluteSchemaLocation, alreadyChecked);
            if (found != null) {
                return found;
            }
        }
        return null;
    }


    /**
     *
     * @param typeName the qname of the type
     * @return the type
     * @throws XsdDefinitionException if the type could not be resolved  or if there was a problem in an XSD
     */
    public XsdType findType(QualifiedName typeName) throws XsdDefinitionException {
        if (XSD_NAMESPACE.equals( typeName.getNamespace() )) {
            return new XsdCoreType(this,typeName.getLocalName());
        }
        XsdNode type = findThing(typeName, COMPLEXTYPE, new HashSet<XsdDefinition>());
        if (type != null && type instanceof XsdType) {
            return (XsdType)type;
        }
        type = findThing(typeName, SIMPLETYPE, new HashSet<XsdDefinition>());
        if (type != null && type instanceof XsdType) {
            return (XsdType)type;
        }
        throw new TypeNotFoundException( typeName.getNamespace(), typeName.getLocalName() );
    }

    /**
     *
     * @param groupName the qname of the group
     * @return the group
     * @throws XsdDefinitionException if the type could not be resolved or if there was a problem in an XSD
     */
    public XsdGroup findGroup(QualifiedName groupName) throws XsdDefinitionException {
        XsdGroup group = findThing(groupName, GROUP, new HashSet<XsdDefinition>());
        if (group != null) {
            return group;
        }
        throw new GroupNotFoundException( groupName.getNamespace(), groupName.getLocalName() );
    }

    /**
     *
     * @param elementName the qname of the group
     * @return the element
     * @throws XsdDefinitionException if the type could not be resolved or if there was a problem in an XSD
     */
    public XsdElement findElement(QualifiedName elementName) throws XsdDefinitionException {
        XsdElement element = findThing(elementName, ELEMENT, new HashSet<XsdDefinition>());
        if (element != null) {
            return element;
        }
        throw new ElementNotFoundException( elementName.getNamespace(), elementName.getLocalName() );
    }

    /**
     *
     * @param attributeName the qname of the group
     * @return the element
     * @throws XsdDefinitionException if the type could not be resolved or if there was a problem in an XSD
     */
    public XsdAttribute findAttribute(QualifiedName attributeName) throws XsdDefinitionException {
        XsdAttribute attribute = findThing(attributeName, ATTRIBUTE, new HashSet<XsdDefinition>());
        if (attribute != null) {
            return attribute;
        }
        throw new AttributeNotFoundException( attributeName.getNamespace(), attributeName.getLocalName() );
    }

    /**
     *
     * @param attributeGroupName the qname of the attribute group
     * @return the element
     * @throws XsdDefinitionException if the type could not be resolved or if there was a problem in an XSD
     */
    public XsdAttributeGroup findAttributeGroup(QualifiedName attributeGroupName) throws XsdDefinitionException {
        XsdAttributeGroup attributeGroup = findThing(attributeGroupName, ATTRIBUTEGROUP, new HashSet<XsdDefinition>());
        if (attributeGroup != null) {
            return attributeGroup;
        }
        throw new AttributeGroupNotFoundException( attributeGroupName.getNamespace(), attributeGroupName.getLocalName() );
    }


    /**
     * Common finder method for things in the schema
     * @param thingName the type name or group name
     * @param element the schema tag eg &lt;element&gt;  or &lt;group&gt;
     * @param <T>
     * @return
     * @throws XsdDefinitionException
     */
    private <T extends XsdNode> T findThing(QualifiedName thingName, SchemaElements element, Set<XsdDefinition> alreadyProcessed) throws XsdDefinitionException {
        if (!alreadyProcessed.add(this)) {
            return null;
        }
        /**
         * We can match iff
         * either our document has no target namespace and thingName is unqualified (null target namespace)
         * or
         * our document has a target namespace which matches the namespace of thingName
         *
         * Except... if the thingName has no defined namespace, and we define a targetNamespace, than
         * definitions in this document should use the targetNamespace to qualify this thingName.
         */
        if (Objects.equals(thingName.getNamespace(), getTargetNamespace())) {
            List<Element> elements = getChildElements(schemaElement(), elementWithLocalName( element.getLocalName() ), elementWithAttributes("name", thingName.getLocalName()));
            if (elements.size() > 1) {
                throw new InvalidXsdException("Xsd contains multiple definitions of " + element.getLocalName() + " '" + thingName.getLocalName() + "'");
            }
            if (elements.size() == 1) {
                @SuppressWarnings("unchecked")
                T value = (T)Common.toXsdNode(this, elements.get(0));
                return value;
            }
            else {
                //check for the thing in our includes
                T thing  = findInDefinitions(includes, thingName, element, alreadyProcessed);
                if (thing != null) {
                    return thing;
                }
            }
        }

        /**
         * The thing is still  not found so
         * search in our imports
         *
         */
        return findInDefinitions(imports, thingName, element, alreadyProcessed);
    }

    private static <T extends XsdNode> T findInDefinitions(Collection<XsdDefinition> definitions, QualifiedName thingName, SchemaElements element, Set<XsdDefinition> alreadyProcessed) throws XsdDefinitionException {
        for (XsdDefinition definition: definitions) {
            T thing = definition.findThing( thingName, element, alreadyProcessed );
            if (thing != null) {
                return thing;
            }
        }
        return null;
    }

    /**
     * looks at the documents which include (and import) me (recursive)
     * to find all of the substitutions.
     * @throws XsdDefinitionException
     */
    public List<XsdElement> findSubstitions(XsdElement xsdElement) throws XsdDefinitionException {
        List<XsdElement> result = new LinkedList<>();
        _findSubstitions(xsdElement, new HashSet<XsdDefinition>(),  new HashSet<Node>(), result);
        return result;
    }

    private void _findSubstitions(XsdElement xsdElement, Set<XsdDefinition> alreadyProcessed, Set<Node> alreadyFound, List<XsdElement> result) throws XsdDefinitionException {
        if (!alreadyProcessed.add(this)) {
            return;
        }
        /*
         * an element which is in a substituiongroup must be declared on the top level of a schema definition
         */
        for (XsdElement childElement: getChildTargetElements()) {
            QualifiedName qname = childElement.getSubstitutionGroup();
            if (qname != null) {
                /*
                 * we verify that each and every substitution group qname is resolvable
                 * as a safetly precaution.
                 *
                 * an exception would be thrown if the element was not found.
                 */
                childElement.getXsdDefinition().findElement(qname);
                if (xsdElement.matches(qname)) {
                    LOG.debug("{} Found substitution {} for {}", new Object[]{getSchemaLocation(), childElement.getElementName(), xsdElement.getElementName()});
                    if (alreadyFound.add(childElement.getDomElement())) {
//                        System.out.println("adding " + childElement.getElementName());
                        result.add(childElement);
                    }
//                    else {
//                        System.out.println("already found " + childElement.getElementName());
//                    }
                }
            }
        }
//we don't need to check our includes because getChildTargetElements already returns included child elements.
//        for (XsdDefinition def: includes) {
//            def._findSubstitions(xsdElement, alreadyProcessed, alreadyFound, result);
//        }
        for (XsdDefinition def: includesMe) {
            def._findSubstitions(xsdElement, alreadyProcessed, alreadyFound, result);
        }
        for (XsdDefinition def: imports) {
            def._findSubstitions(xsdElement, alreadyProcessed, alreadyFound, result);
        }
        for (XsdDefinition def: importsMe) {
            def._findSubstitions(xsdElement, alreadyProcessed, alreadyFound, result);
        }
    }

    /**
     * Resolves the namespace prefix.
     * @param prefix
     * @return the namespace url or null if it could not be resolved.
     */
    String resolvePrefix(String prefix) {
        return getAttribute(schemaElement(), "xmlns:" + prefix, null);
    }

    private static Document parse(byte xsdData[]) throws XsdDefinitionException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().parse(new ByteArrayInputStream( xsdData ));
        }
        catch(Exception x) {
            throw new XsdDefinitionException("Could not create XSD Definition", x);
        }
    }

    private void validateInclude(Element element) throws InvalidXsdException {
        if (element.getAttribute("schemaLocation").isEmpty()) {
            throw new InvalidXsdException("schemaLocation must be specified on include");
        }
    }

    private Element schemaElement() {
        return getChildElement(document);
    }

    @Override
    public XsdNode getParent() {
        return null; // Note: may well be null, e.g. if this XsdDefinition was not included or imported
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        document = parseDocument( stream );
        xsdAnyContent = new HashMap<Node, XsdNode>();
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        serializeDocument(stream, document);
    }

    private void serializeDocument(ObjectOutputStream stream, Document document) throws IOException {
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            trans.transform(new DOMSource(document), new StreamResult( bout ));
            byte data[] = bout.toByteArray();
            stream.writeInt(data.length);
            stream.write(data, 0, data.length);

        } catch (TransformerFactoryConfigurationError | TransformerException x) {
            throw new IOException("Could not serilalize DOM to byte stream", x);
        }

    }
    private Document parseDocument(ObjectInputStream stream) throws IOException {
        int documentByteSize = stream.readInt();
        byte data[] = new byte[ documentByteSize ];
        stream.readFully(data);
        try {
            return parse( data  );
        }
        catch (XsdDefinitionException e) {
            throw new IOException("Error parsing XSD document");
        }
    }
    @Override
    public String toString() {
        return "XsdDefinition[ " + schemaLocation + "]";
    }
}


class DefaultDefinitionResolver implements XsdDefinition.DefinitionResolver {

    @Override
    public String getAbsoluteSchemaLocation(XsdDefinition forDefinition, String relativeSchemaLocation) {
        throw new UnsupportedOperationException("A more specific definition resolver is required.");
    }

    public XsdDefinition resolveInclude(XsdDefinition def, String schemaLocation) throws SchemaNotFoundException, InvalidXsdException {
        throw new SchemaNotFoundException(DependencyType.INCLUDE, null, schemaLocation);
    }

    public Set<XsdDefinition> resolveImport(XsdDefinition def, String schemaLocation, String namespace) throws SchemaNotFoundException, InvalidXsdException {
        throw new SchemaNotFoundException(DependencyType.IMPORT, namespace, schemaLocation);
    }

}
