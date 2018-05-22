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
import static scott.barleydb.xsd.SchemaElements.SIMPLETYPE;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.XsdDefinitionException;

/**
 *
 * The attribute element defines an attribute
 *
 * Parent elements: attributeGroup, schema, complexType, restriction (both simpleContent and complexContent), extension (both simpleContent and complexContent)
 *
 *
 * <attribute
 * default=string
 * fixed=string
 * form=qualified|unqualified
 * id=ID
 * name=NCName
 * ref=QName
 * type=QName optional
 * use=optional|prohibited|required
 * any attributes
 * >
 *
 * (annotation?,(simpleType?))
 *
 * </attribute>
 *
 * An instance of this class can be a direct attribute definition, or a reference to an attribute definition
 *
 * As Per w3c, When no simple type definition is referenced or provided, the default is ·xs:anySimpleType·, which imposes no constraints at all.
 *
 */
public class XsdAttribute implements XsdNode {
    private final XsdNode parent;
    private final Element domElement;

    public XsdAttribute(XsdNode parent, Element domElement) {
        this.parent = parent;
        this.domElement = domElement;
    }

    @Override
    public Node getDomNode() {
      return domElement;
    }

    @Override
    public XsdDefinition getXsdDefinition() {
        return parent.getXsdDefinition();
    }

    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        XsdAttribute ref = resolveReference();
        if (ref != null) {
            return ref.getDocumentation();
        }
        return Common.getDocumentation(domElement);
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

    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    public String getName() throws XsdDefinitionException {
        XsdAttribute ref = resolveReference();
        if (ref != null) {
            return ref.getName();
        }
        String name = domElement.getAttribute("name");
        if (!name.isEmpty()) {
            return name;
        }
        throw new InvalidXsdException("Attribute must have a ref or name defined");
    }

    /**
     * gets the namespace of the attribute
     *
     * @return
     * @throws XsdDefinitionException
     */
    public String getNamespaceUri() throws XsdDefinitionException {
        XsdAttribute ref = resolveReference();
        if (ref != null) {
            return ref.getNamespaceUri();
        }
        return getXsdDefinition().getTargetNamespace();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        final String ref = domElement.getAttribute("ref");
        if (!ref.isEmpty()) {
            return "<attribute ref=\"" + ref + "\"/>";
        }
        else {
            return "<attribute name=\"" + domElement.getAttribute("name") + "\"/>";
        }
    }

    /**
     * If we are an element reference, then return the resolved element
     * @return
     */
    private XsdAttribute resolveReference() throws XsdDefinitionException {
        String ref = domElement.getAttribute("ref");
        if (ref.isEmpty()) {
            return null;
        }
        return getXsdDefinition().findAttribute(new QualifiedName(getXsdDefinition(), ref));
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }

    /**
    *
    * @return the name of the type in the type attribute
    */
   public String getTypeName() throws XsdDefinitionException {
       XsdAttribute ref = resolveReference();
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
       XsdAttribute ref = resolveReference();
       if (ref != null) {
           return ref.getType();
       }
       String type = domElement.getAttribute("type");
       if (type.isEmpty()) {
           XsdNode xsdType = Common.getXsdNode(this, domElement, SIMPLETYPE);
           if (xsdType != null && xsdType instanceof XsdType) {
               return (XsdType)xsdType;
           }
           /*
            * no type definition, so the type is 'anySimpleType' as per w3c.
            */
           return getXsdDefinition().findType(new QualifiedName(XsdDefinition.XSD_NAMESPACE, "anySimpleType"));
       }
       else {
           return getXsdDefinition().findType(new QualifiedName(getXsdDefinition(), type, true));
       }
   }

   public String getUse() throws XsdDefinitionException {
       String use = domElement.getAttribute("use");
       if (use.isEmpty()) {
           return "optional";
       }
       return use;
   }

}
