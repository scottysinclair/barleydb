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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.XsdDefinitionException;


/**
 * Walks the target element hierarchy of a graph of XsdDefinitions.
 *
 *
 * Substitution Groups are included by default
 * Asbtract elements are excluded by default.
 *
 * @author scott
 *
 */
public class XsdElementWalker {

    private static Logger LOG = LoggerFactory.getLogger(XsdElementWalker.class);

    private final boolean includeSubstitutionGroups;
    private final boolean excludeAbstractElements;
    private final boolean excludeRecursiveElements;
    private final XsdElementWalker parent;
    private final Map<QualifiedName, List<XsdElement>> substitutionGroupCache;
    private final Map<Element, List<XsdElement>> childTargetElementsCache;
    private final Map<Element, XsdType> typeLookupCache;

    private final XsdNode node;

//    private static int count = 0;

    public XsdElementWalker(XsdDefinition xsdDefinition, boolean includeSubstitutionGroups, boolean excludeAbstractElements, boolean excludeRecursiveElements) {
        this.includeSubstitutionGroups = includeSubstitutionGroups;
        this.excludeAbstractElements = excludeAbstractElements;
        this.excludeRecursiveElements = excludeRecursiveElements;
        this.node = xsdDefinition;
        this.parent = null;
        substitutionGroupCache = new HashMap<>();
        childTargetElementsCache = new HashMap<>();
        typeLookupCache = new HashMap<>();
//        count++;
    }

    public XsdElementWalker(XsdDefinition xsdDefinition) {
        this(xsdDefinition, true, true, true);
    }

    public XsdElementWalker getParent() {
        return parent;
    }

    private XsdElementWalker(XsdElementWalker parentWalker, XsdElement element) {
        this.parent = parentWalker;
        this.includeSubstitutionGroups = parentWalker.includeSubstitutionGroups;
        this.excludeAbstractElements = parentWalker.excludeAbstractElements;
        this.excludeRecursiveElements = parentWalker.excludeRecursiveElements;
        this.substitutionGroupCache = parentWalker.substitutionGroupCache;
        this.childTargetElementsCache = parentWalker.childTargetElementsCache;
        this.typeLookupCache = parentWalker.typeLookupCache;
        this.node = element;
//        count++;
//        if (count % 100 == 0) {
//            System.out.println("COUNT " + count + " at depth " + getDepth());
//        }
    }

    @SuppressWarnings("unused")
    private int getDepth() {
        if (parent == null) {
            return 0;
        }
        return parent.getDepth() + 1;
    }


    public boolean isTopDefinition()  {
        return parent == null;
    }


    /**
     * @return the current element.
     */
    public XsdElement getElement() {
        return (XsdElement)node;
    }

    /**
     * Get the children of this element walker.
     */
    public Collection<XsdElementWalker> getChildren() throws XsdDefinitionException {
        List<XsdElementWalker> result = new LinkedList<>();
        Set<Node> uniqueChildren = new HashSet<>();
        for (XsdElement child: getChildTargetElements(node)) {
            LOG.trace("Processing child element {}  of path {}...", child, getPath());
            if (excludeRecursiveElements && alreadyInsideElementType(child)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Skipping child element {}  of path {} as we are excluding recursive elements and we are already inside it.", child, getPath());
                }
                continue;
            }
            if (!excludeAbstractElements || !child.isAbstract()) {
                if (uniqueChildren.add(child.getDomElement())) {
                    result.add(new XsdElementWalker(this, child));
                }
            }
            if (includeSubstitutionGroups && child.isTopLevel()) {
                for (XsdElement sub: getSubstitutions(child)) {
                    if (excludeRecursiveElements && alreadyInsideElementType(sub)) {
                        continue;
                    }
                    if (!excludeAbstractElements || !sub.isAbstract()) {
                        if (uniqueChildren.add(sub.getDomElement())) {
                            result.add(new XsdElementWalker(this, sub));
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<XsdElement> getChildTargetElements(XsdNode node) throws XsdDefinitionException {
        if (node instanceof XsdDefinition) {
            return node.getChildTargetElements();
        }
        XsdElement element = (XsdElement)node;
        List<XsdElement> children = childTargetElementsCache.get(element.getDomElement());
        if (children == null) {
            children = node.getChildTargetElements();
            childTargetElementsCache.put(element.getDomElement(), children);
        }
//testing
//        else {
//            List<XsdElement> actual = node.getChildTargetElements();
//            if (children.size() != actual.size()) {
//                System.out.println(element.getElementName() + " cache: " + children.size() + ", actual: " + actual.size());
//            }
//        }
        return children;
    }

    private List<XsdElement> getSubstitutions(XsdElement child) throws XsdDefinitionException {
        if (!child.isTopLevel()) {
            return Collections.emptyList();
        }
        List<XsdElement> subs = substitutionGroupCache.get(child.getQualifiedName());
        if (subs == null) {
            subs = child.getSubstitutions();
            substitutionGroupCache.put(child.getQualifiedName(), subs);
//            System.out.println("Went to DOM for substitutionGroup");
        }
        else {
//            System.out.println("Resolved substitutionGroup from cache");
        }
        return subs;
    }

    /**
     * Checks if the given element is higher up in the hierarchy
     */
    private boolean alreadyInsideElementType(XsdElement element) throws XsdDefinitionException {
        return alreadyInsideElementType(element, null);
    }
    /**
     * Checks if the given element is higher up in the hierarchy, path and printRecursion are for testing and are commented out
     */
    private boolean alreadyInsideElementType(XsdElement element, LinkedList<XsdElementWalker> path) throws XsdDefinitionException {
        if (parent == null) {
            return false;
        }
        //path.add(this);
        /*
         * We are the ancestor if we are of the same type as the element parameter.
         * The element name is irrelevant
         *
         * account:Account
         *   porto: Portfolio
         *     subAcc:Account - subAcc should be detected as having an ancestor.
         *
         * we compare the dom node of the element's type definition, this is guaranteed to only
         * match if the element has exactly the same type definition. This will also work for
         * anonymous inner types which have no name.
         *
         */
        if (getType(element).getDomNode() == getType(getElement()).getDomNode()) {
            //printRecursion(element, path);
            return true;
        }
        return parent.alreadyInsideElementType(element, path);
    }

    private XsdType getType(XsdElement element) throws XsdDefinitionException {
        XsdType type = typeLookupCache.get(element.getDomElement());
        if (type == null) {
            type = element.getType();
            typeLookupCache.put(element.getDomElement(), type);
        }
        return type;
    }

    /*
     * prints the path to the given element
    private void printRecursion(XsdElement element, LinkedList<XsdElementWalker> path) throws XsdDefinitionException {
        Collections.reverse(path);
        for (XsdElementWalker el: path) {
            System.out.print(el.getElement().getElementName() + " -> ");
        }
        System.out.println(element.getElementName());
    }
*/
    @Override
    public String toString() {
        return String.valueOf(node);
    }

    public String getPath() throws XsdDefinitionException {
        if (parent == null) {
            return null;
        }
        else {
            String parentPath = parent.getPath();
            if (parentPath == null) {
                return getElement().getElementName();
            }
            else {
                return parentPath + "." + getElement().getElementName();
            }
        }
    }



}
