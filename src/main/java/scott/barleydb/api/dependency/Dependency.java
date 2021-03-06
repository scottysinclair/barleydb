package scott.barleydb.api.dependency;

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

/**
 * @author scott
 *
 */
public class Dependency {
    private final DependencyTreeNode from;
    private final DependencyTreeNode to;
    private final Object thing;
    public Dependency(DependencyTreeNode from, DependencyTreeNode to, Object thing) {
        this.from = from;
        this.to = to;
        this.thing = thing;
    }
    public DependencyTreeNode getFrom() {
        return from;
    }
    public DependencyTreeNode getTo() {
        return to;
    }
    public Object getThing() {
        return thing;
    }
}
