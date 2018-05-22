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

import static scott.barleydb.xsd.DomHelper.getAttribute;
import static scott.barleydb.xsd.SchemaElements.COMPLEXTYPE;
import static scott.barleydb.xsd.SchemaElements.SIMPLETYPE;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.XsdDefinitionException;

/*
* <element
* id=ID
* name=NCName
* ref=QName
* type=QName
* substitutionGroup=QName
* default=string
* fixed=string
* form=qualified|unqualified
* maxOccurs=nonNegativeInteger|unbounded
* minOccurs=nonNegativeInteger
* nillable=true|false
* abstract=true|false
* block=(#all|list of (extension|restriction))
* final=(#all|list of (extension|restriction))
* any attributes
* >
*
* annotation?,(simpleType|complexType)?,(unique|key|keyref)*
*
* </element>
 */
public final class XsdElement implements XsdNode {

    private final XsdNode parent;
    private final Element domElement;

    public XsdElement(XsdNode parent, Element domElement) {
        this.parent = parent;
        this.domElement = domElement;
    }

    @Override
    public Node getDomNode() {
      return domElement;
    }


    public Element getDomElement() {
        return domElement;
    }

    /**
     * gets the name of this element, if we are an element reference then we resolve the reference
     * @return
     * @throws XsdDefinitionException
     */
    public String getElementName() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getElementName();
        }
        String name = domElement.getAttribute("name");
        if (!name.isEmpty()) {
            return name;
        }
        throw new InvalidXsdException("element must have either the name or ref attribute");
    }

    /**
     * @return true if the element is abstract
     * @throws XsdDefinitionException
     */
    public boolean isAbstract() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.isAbstract();
        }
        return getAttribute(domElement, "abstract", null) != null;
    }

    public String getElementNamespace() throws XsdDefinitionException {
        return getNamespaceUri();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getAttributes();
        }
        return getType().getAttributes();

    }

    /**
     * Gets the namespace uri of this element
     * If this instance is an element reference, then the namespace returned is
     * the namespace of the element definition which is being referred.
     *
     * @return the namespaceuri which can be null for  no namespace
     */
    public String getNamespaceUri() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getNamespaceUri();
        }
        return getXsdDefinition().getTargetNamespace();
    }

    /**
     * @return the substitionGroup of this element as a qualified name.
     */
    public QualifiedName getSubstitutionGroup() {
        String substitutionGroup = domElement.getAttribute("substitutionGroup");
        if (substitutionGroup.isEmpty()) {
            return null;
        }
        return new QualifiedName(getXsdDefinition(), substitutionGroup);
    }


    /**
     * Gets the documentation of this element and falls back to the documentation
     * of the type .
     * @return the documentation of the element or type or the empty list
     * @throws XsdDefinitionException
     */
    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getDocumentation();
        }
        List<XsdDocumentation> documentation =  Common.getDocumentation(domElement);
        return documentation.isEmpty() ? getType().getDocumentation() : documentation;
    }

    /**
     * Returns the documentation with the given source
     * @param source
     * @return the documentation with the given source or null if not found
     * @throws XsdDefinitionException if there is a problem with the XSD
     * @throws NullPointerException if source is null
     */
    public XsdDocumentation getDocumentationWithSource(String source) throws XsdDefinitionException {
        return Common.getDocumentationWithSource(getDocumentation(), source);
    }

    /**
     *
     * @return the max number of occurrences
     * @throws XsdDefinitionException
     */
    public Integer getMinOccurs() throws XsdDefinitionException {
        //we should prioritise the attribute on the domElement so that the ref minOccurs will
        //override the element definition of maxOccurs
        String minOccurs = domElement.getAttribute("minOccurs");
        if (!minOccurs.isEmpty()) {
            try {
                return Integer.parseInt( minOccurs );
            }
            catch(NumberFormatException x) {
                throw new XsdDefinitionException("Invalid minOccurs value '" + minOccurs + "'");
            }
        }
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getMinOccurs();
        }
        //the value defaults to 1
        return 1;
    }

    /**
     *
     * @return the max number of occurrences Integer.MAX_VALUE indicates unbounded
     * @throws XsdDefinitionException
     */
    public Integer getMaxOccurs() throws XsdDefinitionException {
        //we should prioritise the attribute on the domElement so that the ref maxOccurs will
        //override the element definition of maxOccurs
        String maxOccurs = domElement.getAttribute("maxOccurs");
        if (!maxOccurs.isEmpty()) {
            try {
                return "unbounded".equalsIgnoreCase(maxOccurs) ? Integer.MAX_VALUE : Integer.parseInt( maxOccurs );
            }
            catch(NumberFormatException x) {
                throw new XsdDefinitionException("Invalid maxOccurs value '" + maxOccurs + "'");
            }
        }
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getMaxOccurs();
        }
        //the value defaults to 1
        return 1;
    }

    /**
     * Gets the child elements of this element
     * @return
     * @throws scott.barleydb.xsd.exception.XsdDefinitionException
     */
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getChildTargetElements();
        }
        return getType().getChildTargetElements();
    }

    /**
     * @return true if the element is a top level definition.
     * @throws XsdDefinitionException
     */
    public boolean isTopLevel() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.isTopLevel();
        }
        return parent instanceof XsdDefinition;
    }

    /**
     * Gets all substitutions of this element, including substitutions of substitutions.
     * @throws XsdDefinitionException
     */
    public List<XsdElement> getSubstitutions() throws XsdDefinitionException {
        if (!isTopLevel()) {
            /*
             * see http://www.w3.org/TR/xmlschema11-1/#Element_Equivalence_Class
             *  top-level element declaration can serve as the defining member, or head, for an element
             *  ·substitution group·. Other top-level element declarations, regardless of target namespace,
             *  can be designated as members of the ·substitution group· headed by this element
             */
            return Collections.emptyList();
        }

        List<XsdElement> result = new LinkedList<>();
        _getSubstitutions(new HashSet<String>(), result);
        return result;
    }

    private void _getSubstitutions(Set<String> addedToResult, List<XsdElement> result) throws XsdDefinitionException {
        List<XsdElement> subs = getXsdDefinition().findSubstitions(this);
        for (XsdElement sub: subs) {
            if (addedToResult.add(sub.getElementNamespace() + ":" + sub.getElementName())) {
                result.add(sub);
                sub._getSubstitutions(addedToResult, result);
            }
        }
    }

    /**
     *
     * @return the name of the type in the type attribute
     */
    public String getTypeName() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getTypeName();
        }
        return getAttribute(domElement, "type", null);
    }

    /**
     *
     * @return the type of the element
     * @throws XsdDefinitionException  if the type could not be resolved
     * @throws  XsdDefinitionException if no type is defined
     */
    public XsdType getType() throws XsdDefinitionException {
        XsdElement ref = resolveReference();
        if (ref != null) {
            return ref.getType();
        }
        String type = domElement.getAttribute("type");
        if (type.isEmpty()) {
            XsdNode xsdType = Common.getXsdNode(this, domElement, COMPLEXTYPE, SIMPLETYPE);
            if (xsdType != null && xsdType instanceof XsdType) {
                return (XsdType)xsdType;
            }
            throw new XsdDefinitionException("element does not reference or define a type");
        }
        else {
            return getXsdDefinition().findType( new QualifiedName(getXsdDefinition(), type, true) );
        }
    }

    public final QualifiedName getQualifiedName() throws XsdDefinitionException {
        return new QualifiedName(getXsdDefinition(), getElementName());
    }

    /**
     * If we are an element reference, then return the resolved element
     * @return
     */
    private XsdElement resolveReference() throws XsdDefinitionException {
        String ref = domElement.getAttribute("ref");
        if (ref.isEmpty()) {
            return null;
        }
        return getXsdDefinition().findElement(new QualifiedName(getXsdDefinition(), ref));
    }

    /**
     * can be the xsd definition of an element reference
     * can be the xsd definition of an element definition
     * @return the xsd definition which declares us
     */
    @Override
    public XsdDefinition getXsdDefinition() {
        return parent.getXsdDefinition();
    }

    @Override
    public String toString() {
        final String ref = domElement.getAttribute("ref");
        if (!ref.isEmpty()) {
            return "<xs:element ref=\"" + ref + "\"/>";
        }
        return "<xs:element name=\"" + domElement.getAttribute("name") + "\"/>";
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }

    public boolean matches(QualifiedName qname) throws XsdDefinitionException {
        return Objects.equals(getNamespaceUri(), qname.getNamespace()) && qname.getLocalName().equals(getElementName());
    }

}
