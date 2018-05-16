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

import org.w3c.dom.Element;

import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.XsdDefinitionException;

import java.util.*;

import static scott.barleydb.xsd.DomHelper.*;

public class Common {

    /**
     * gets documentation annotated on a dom element
     * @param domElement
     * @return the list of documentation or the emptylist
     */
    public static List<XsdDocumentation> getDocumentation(Element domElement) {
        List<XsdDocumentation> documentation = null;
        Element annotation = getChildElement(domElement, elementWithLocalName("annotation"));
        if (annotation != null) {
            for (Element documentationElement: getChildElements(annotation, elementWithLocalName("documentation"))) {
                if (documentation == null) {
                    documentation = new LinkedList<>(); //lazy init
                }
                documentation.add(new XsdDocumentation(documentationElement));
            }
        }
        return documentation != null ? documentation : Collections.<XsdDocumentation>emptyList();
    }

    /**
     * Returns the documentation with the given source
     * @param source
     * @return the documentation with the given source or null if not found
     * @throws XsdDefinitionException if there is a problem with the XSD
     * @throws NullPointerException if source is null
     */
    public static XsdDocumentation getDocumentationWithSource(List<XsdDocumentation> documentationList, String source) throws XsdDefinitionException {
        if (source == null) {
            throw new NullPointerException(); //prevents us returning  documentation with null source
        }
        for (XsdDocumentation documentation: documentationList) {
            if (source.equals( documentation.getSource() )) {
                return documentation;
            }
        }
        return null;
    }


    /**
     * gets all XsdNodes which match the given list of element names
     * @param parent
     * @param domElement
     * @param elements
     * @return the list of nodes or empty list if none matched
     * @throws InvalidXsdException
     */
    public static List<XsdNode> getXsdNodes(XsdNode parent, Element domElement, SchemaElements... elements) throws InvalidXsdException {
        List<XsdNode> result = new LinkedList<>();
        for (Element element :getChildElements(domElement, oneOf(toConditions(elements)))) {
            result.add( toXsdNode(parent, element));
        }
        return result;
    }


    /**
     * Gets the first xsd dom element specified in 'elementNames' as a XsdNode and returns it if it is an XsdElement, otherwise returns it's children
     * @param parent
     * @param domElement
     * @param elements
     * @return the list of xsdelements or the empty list if there are none
     * @throws XsdDefinitionException
     */
    public static List<XsdElement> getChildElementsFromOneOf(XsdNode parent, Element domElement, SchemaElements ...elements) throws XsdDefinitionException {
        XsdNode child = getXsdNode(parent, domElement, elements);
        if (child instanceof XsdElement) {
            return new ArrayList<>(Arrays.asList(new XsdElement[]{(XsdElement)child}));
        }
        else if (child != null) {
            return child.getChildTargetElements();
        }
        return Collections.emptyList();
    }

    /**
     * Adds all XsdNodes which match 'elementNames' into collection C,
     * Then for each XsdElement E in collection C, adds E to the result list R
     * Then for each non XsdElement N in collection C, adds the child XsdElements of N to the result list R
     *
     * @param parent
     * @param domElement
     * @param elements
     * @return the list of XsdElements or the empty list if none are available
     * @throws XsdDefinitionException
     */
    public static List<XsdElement> getChildElementsAcrossNodes(XsdNode parent, Element domElement, SchemaElements... elements) throws XsdDefinitionException {
        List<XsdElement> childTargetElements = new LinkedList<>();
        for (XsdNode child: getXsdNodes(parent, domElement, elements)) {
            if (child instanceof XsdElement) {
                childTargetElements.add((XsdElement)child);
            }
            else {
                childTargetElements.addAll( child.getChildTargetElements() );
            }
        }
        return childTargetElements;
    }

    /**
     * collects attributes definitions from children of 'parent'
     * if any children are of type attributeGroup then the attributes from these groups will also be returned
     * @param parent
     * @param domElement
     * @return
     * @throws XsdDefinitionException
     */
    public static List<XsdAttribute> getAttributes(XsdNode parent, Element domElement, SchemaElements ...elements) throws XsdDefinitionException {
        List<XsdAttribute> result = new LinkedList<>();
        for (XsdNode child: Common.getXsdNodes(parent, domElement, elements)) {
            if (child instanceof XsdAttribute) {
            /*
             * We directly contain an attribute so add it
             */
                result.add( (XsdAttribute)child );
            }
            else if (!(child instanceof XsdElement)) {
                /**
                 * get the attributes of any child definitions assuming it applies to us
                 * exclude XsdElement children as we know that any attributes from them apply to their own elements.
                 */
                result.addAll( child.getAttributes() );
            }
        }
        return result;
    }


    /**
     * gets the first child xsd dom element matches 'elementNames'
     * @param parent
     * @param domElement
     * @param elements
     * @return
     * @throws InvalidXsdException
     */
    public static XsdNode getXsdNode(XsdNode parent, Element domElement, SchemaElements... elements) throws InvalidXsdException {
        Element element = getChildElement(domElement, oneOf( toConditions(elements) ));
        if (element != null) {
            return toXsdNode(parent, element);
        }
        return null;
    }

    private static WithCondition[] toConditions(SchemaElements elements[]) {
        WithCondition conditions[] = new WithCondition[ elements.length ];
        for (int i=0; i<elements.length; i++) {
            conditions[i] = elementWithLocalName(elements[i].getLocalName());
        }
        return conditions;
    }

    public static XsdNode toXsdNode(XsdNode parent, Element element) throws InvalidXsdException {
        switch(SchemaElements.toSchemaElement( element.getLocalName())) {
            case ELEMENT: return new XsdElement(parent, element);
            case ATTRIBUTE: return new XsdAttribute(parent, element);
            case ATTRIBUTEGROUP: return new XsdAttributeGroup(parent, element);
            case COMPLEXTYPE: return new XsdComplexType(parent, element);
            case COMPLEXCONTENT: return new XsdComplexContent(parent, element);
            case SIMPLETYPE: return new XsdSimpleType(parent, element);
            case SIMPLECONTENT: return new XsdSimpleContent(parent, element);
            case GROUP: return new XsdGroup(parent, element);
            case ALL: return new XsdAll(parent, element);
            case CHOICE: return new XsdChoice(parent, element);
            case SEQUENCE: return new XsdSequence(parent, element);
            case ANY: return new XsdAny(parent, element);
            case RESTRICTION: return new XsdRestriction(parent, element);
            case EXTENSION: return new XsdExtension(parent, element);
            default: throw new InvalidXsdException(element.getLocalName() + " is not supported");
        }
    }

    public static String composeUriQualifiedName(String nsUri, String localName) {
        StringBuilder buf = new StringBuilder();
        int p;
        if ((p=localName.lastIndexOf(':')) != -1) {
            if (p==localName.length()-1) throw new IllegalArgumentException(localName); // No XML name may end with ':'
            buf.append(localName.substring(p+1));
        } else {
            buf.append(localName);
        }
        buf.insert(0,':');
        if (nsUri != null && !nsUri.isEmpty()) {
            buf.insert(0, nsUri);
        } else {
            buf.insert(0, "urn:noNamespace");
        }
        return buf.toString();
    }

}
