package scott.sort.api.core.proxy;

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

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.core.entity.RefNode;

public class RefNodeProxyHelper {

    private Object refProxy;
    public final RefNode refNode;

    public RefNodeProxyHelper(RefNode refNode) {
        this.refNode = refNode;
    }

    @Override
    public String toString() {
        Entity ref = refNode.getReference();
        if (ref == null) {
            refProxy = null;
        }
        else if (refProxy == null || ((ProxyController) refProxy).getEntity() != ref) {
            refProxy = ref.getEntityContext().getProxy(ref);
        }
        return String.valueOf(ref);
    }

}
