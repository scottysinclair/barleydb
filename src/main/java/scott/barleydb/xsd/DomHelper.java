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


import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DomHelper {

    public interface WithCondition  {
        public boolean withCondition(Node node);
    }

    public interface Executer<T extends Node, E extends Exception> {
        public void execute(T node) throws E;
    }

    public static WithCondition oneOf(final WithCondition ...conditions) {
        return new WithCondition() {
            @Override
            public boolean withCondition(Node node) {
                for (WithCondition c: conditions) {
                    if (c.withCondition(node))  {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static WithCondition elementWithAttributes(final String attrName, final String attrValue) {
        return new WithCondition() {
            @Override
            public boolean withCondition(Node node) {
                return isElementWithAttribute(node, attrName, attrValue);
            }
        };
    }

    public static WithCondition elementWithTagName(final String tagName) {
        return new WithCondition() {
            @Override
            public boolean withCondition(Node node) {
                return node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals( tagName );
            }
        };
    }

    public static WithCondition elementWithLocalName(final String tagName) {
        return new WithCondition() {
            @Override
            public boolean withCondition(Node node) {
                return node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getLocalName());
            }
        };
    }

    public static WithCondition elementWithAttributeName(final String attrName) {
        return new WithCondition() {
            @Override
            public boolean withCondition(Node node) {
                return isElementWithAttributeName(node, attrName);
            }
        };
    }


    public static WithCondition nodeNameMatches(String... names) {
        final Collection<String> collection = Arrays.asList(names);
        return new WithCondition() {
            @Override
            public boolean withCondition(Node node) {
                return collection.contains( node.getNodeName() );
            }
        };
    }

    public static<E extends Exception> void executeOnChildElements(Node parentNode, Executer<Element,E> executer, WithCondition... conditions) throws E {
        for (Node node: getChildNodes(parentNode, conditions)) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                executer.execute((Element)node);
            }
        }
    }


    public static Element getElementAncestorWithCondition(Node node, WithCondition... conditions) {
        Node parent = node;
        while(parent != null) {
            if (parent.getNodeType() == Node.ELEMENT_NODE && checkConditions(parent, conditions)) {
                return (Element)parent;
            }
            parent = parent.getParentNode();
        }
        return null;
    }


    /**
     *
     * @param parentNode
     * @param conditions
     * @return the first matching element or null.
     */
    public static Collection<Node> getChildNodes(Node parentNode, WithCondition... conditions) {
        Collection<Node> nodes = new LinkedList<Node>();
        NodeList nl =  parentNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (checkConditions(node, conditions)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public static Element getRootElement(Node node) {
        Document doc = getOwnerDocument(node);
        return getChildElement(doc);
    }

    /**
     * the first child element which matches the conditions
     * @param parentNode
     * @param conditions
     * @return the first matching element or null.
     */
    public static Element getChildElement(Node parentNode, WithCondition... conditions) {
        NodeList nl =  parentNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (checkConditions(node, conditions)) {
                return (Element)node;
            }
        }
        return null;
    }

    public static List<Element> getDecendentElements(Node parentNode, WithCondition... conditions) {
        return getChildElements(parentNode, true, conditions);
    }

    public static List<Element> getChildElements(Node parentNode, WithCondition... conditions) {
        return getChildElements(parentNode, false, conditions);
    }

    private static List<Element> getChildElements(Node parentNode, boolean recursive, WithCondition... conditions) {
        List<Element> elements = new LinkedList<Element>();
        NodeList nl =  parentNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (recursive) {
                elements.addAll( getChildElements(node, true, conditions));
            }
            if (checkConditions(node, conditions)) {
                elements.add( (Element)node );
            }
        }
        return elements;
    }

    private static boolean checkConditions(Node node, WithCondition... conditions) {
        if (conditions == null) {
            return true;
        }
        for (WithCondition c: conditions) {
            if (!c.withCondition(node)) {
                return false;
            }
        }
        return true;
    }

    public static Element getChildElementWithAttribute(Node parentNode, String attrName, String attrValue) {
        NodeList nl =  parentNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isElementWithAttribute(node, attrName, attrValue)) {
                return (Element)node;
            }
        }
        return null;
    }


    /**
     *
     * @param node
     * @param attrName
     * @param attrValue
     * @return true if the node is an element with the specified attribute value
     */
    public static boolean isElementWithAttribute(Node node, String attrName, String attrValue) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        Element el = (Element)node;
        return attrValue.equals( el.getAttribute(attrName) );
    }

    public static boolean isElementWithAttributeName(Node node, String attrName) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        Element el = (Element)node;
        return el.getAttribute(attrName).length() > 0;
    }

    public static void appendText(Node parent, String text) {
        Node node = getOwnerDocument(parent).createTextNode(text);
        parent.appendChild(node);
    }


    public static Element createAndAppendElement(String name, Node parent) {
        Element el = getOwnerDocument(parent).createElement(name);
        parent.appendChild(el);
        return el;
    }

    public static Element createAndAppendElement(String namespace, String name, Node parent) {
        Element el = getOwnerDocument(parent).createElementNS(namespace, name);
        parent.appendChild(el);
        return el;
    }


    public static Document getOwnerDocument(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            return (Document)node;
        }
        return node.getOwnerDocument();
    }

    /**
     * counts how deep in the hierachy the node is.
     * @param node
     * @return
     */
    public static int countParents(Node node) {
        int count = 0;
        while (node != null && node.getNodeType() != Node.DOCUMENT_NODE) {
            node = node.getParentNode();
            count++;
        }
        return count;
    }

    public static String calculateXPath(Element element) {
        try {
            return calculateXPathRelativeToAncestor(element, getOwnerDocument(element));
        }
        catch(IllegalArgumentException x) {
            throw new IllegalStateException("Unexpected exception", x);
        }
    }

    public static String calculateXPathRelativeToAncestor(Element element, Node ancestor) {
        Document doc = getOwnerDocument(element);
        List<String> parts = new LinkedList<String>();
        parts.add( element.getNodeName() );

        Node parent = element.getParentNode();
        while (parent != ancestor) {
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                parts.add(0, parent.getNodeName());
            }
            parent = parent.getParentNode();
            if (parent == doc && ancestor != doc) {
                throw new IllegalArgumentException("'" + element.getTagName() + "' is not a decendent of '" + ancestor + "'");
            }
        }

        StringBuilder xpath = new StringBuilder();
        if (ancestor == doc) {
            xpath.append( '/' );
        }
        for (String part: parts) {
            xpath.append( part );
            xpath.append( '/' );
        }
        xpath.setLength( xpath.length() - 1);
        return xpath.toString();
    }

    public static void removeChildren(Node node) {
        NodeList nl = node.getChildNodes();
        if (nl.getLength() > 0) {
            for (int i=0; i<nl.getLength(); i++) {
                node.removeChild(nl.item(i));
            }
        }
    }

    public static void copyChildren(Node fromNode, Node toNode) {
        for (Element child: getChildElements(fromNode)) {
            fromNode.removeChild( child );
            toNode.appendChild( child );
        }
    }

    public static String getAttribute(Element element, String attribute, String defaultValue) {
        String v = element.getAttribute(attribute);
        return v.isEmpty() ? defaultValue : v;
    }

    public static void moveAttributesIfExistNoOverwrite(Element fromE, Element toE, String ...attributes) {
        for (String attrName: attributes) {
            String value = fromE.getAttribute( attrName );
            if (value.length() > 0) {
                if (toE.getAttribute( attrName ).length() > 0) {
                    throw new IllegalArgumentException("Attribute '" + attrName + "' is already set on element '" + toE.getTagName() + "'");
                }
                toE.setAttribute(attrName, value);
                fromE.removeAttribute(attrName);
            }
        }
    }

}
