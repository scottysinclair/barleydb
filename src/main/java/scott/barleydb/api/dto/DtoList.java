package scott.barleydb.api.dto;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
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

import java.util.LinkedList;

public class DtoList<T> extends LinkedList<T> {

  private static final long serialVersionUID = 1L;

  /**
   * if the list has been fetched or not (true, false or undefined)
   */
  private Boolean fetched;

  public DtoList() {
    super();
  }


   public Boolean isFetched() {
    return fetched;
  }

  public void setFetched(Boolean value) {
    this.fetched = value;
  }
}
