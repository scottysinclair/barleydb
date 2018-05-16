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

import java.util.Locale;

import org.w3c.dom.Element;

import scott.barleydb.xsd.exception.XsdDefinitionException;

import static scott.barleydb.xsd.DomHelper.*;

/**
 * <documentation
 * source=URI reference
 * xml:lang=language>
 *
 * Any well-formed XML content
 *
 * </documentation>
 */
public class XsdDocumentation {
    private final Element domElement;

    public XsdDocumentation(Element domElement) {
        this.domElement = domElement;
    }

    public String getSource() {
        return getAttribute(domElement, "source", null);
    }

    public String getContent(){
        return domElement.getTextContent();
    }

    public String getLanguage() {
        //todo what about the xml: namespace (xml:lang)
        return getAttribute(domElement, "xml:lang", null);
    }

    @Override
    public String toString() {
        return "<documentation source\"" + getSource() + "\" xml:lang=\"" + getLanguage() + "\">" + getContent() + "</documentation>";
    }

    public static String getDocumentationStringForNode(XsdNode node, String source, Locale locale) throws XsdDefinitionException {
        for (XsdDocumentation doc : node.getDocumentation()) {
            if (locale.getLanguage()
                    .equals(new Locale(
                            doc.getLanguage())
                    .getLanguage()) && doc.getSource().equalsIgnoreCase(source)) {
                return doc.getContent();
            }
        }
        return null;
    }
}
