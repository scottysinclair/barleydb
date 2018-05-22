package scott.barleydb.build.specgen.fromxsd;

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

import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.xsd.XsdAttribute;
import scott.barleydb.xsd.XsdElement;
import scott.barleydb.xsd.XsdNode;
import scott.barleydb.xsd.XsdType;
import scott.barleydb.xsd.exception.XsdDefinitionException;

public interface EntitySpecXsdLookup {

  EntitySpec getRequiredEntitySpecFor(XsdType xsdType) throws XsdDefinitionException, IllegalStateException;
  NodeSpec getRequiredNodeSpecFor(XsdType xsdType, XsdElement xsdElement) throws XsdDefinitionException;
  NodeSpec getRequiredNodeSpecFor(XsdType xsdType, XsdAttribute xsdAttribute) throws XsdDefinitionException;

}
