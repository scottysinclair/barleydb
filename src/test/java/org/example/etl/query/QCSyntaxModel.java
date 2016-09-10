package org.example.etl.query;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
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

import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CSyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCSyntaxModel extends QAbstractCSyntaxModel<CSyntaxModel, QCSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QCSyntaxModel() {
    super(CSyntaxModel.class);
  }

  public QCSyntaxModel(QueryObject<?> parent) {
    super(CSyntaxModel.class, parent);
  }

}