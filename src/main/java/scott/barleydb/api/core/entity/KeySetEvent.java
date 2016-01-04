package scott.barleydb.api.core.entity;

import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.NodeEvent;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
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

public class KeySetEvent extends NodeEvent {

    private final Object originalKey;

    public KeySetEvent(Node source, Object originalKey) {
        super(source, NodeEvent.Type.KEYSET);
        this.originalKey = originalKey;
    }

    public Object getOriginalKey() {
        return originalKey;
    }

}
