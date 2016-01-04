package scott.barleydb.api.query;

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

import java.io.Serializable;

/**
 * Runtime properties for a query execution.<br/>
 *<br/>
 * These objects can be passed in with a query during execution.<br/>
 * They decide on how exactly the query executes.
 *
 * @author scott
 *
 */
public class RuntimeProperties implements Serializable {

    public static enum ScrollType {
        FORWARD_ONLY,
        SCROLL_SENSITIVE,
        SCROLL_INSENSITIVE
    }

    public static enum Concurrency {
        READ_ONLY,
        UPDATABLE
    }

    private static final long serialVersionUID = 1L;

    private Integer fetchSize;

    private ScrollType scrollType;

    private Concurrency concurrency;

    private Boolean executeInSameContext;

    public RuntimeProperties fetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    public RuntimeProperties override(RuntimeProperties props) {
        RuntimeProperties rp = new RuntimeProperties();
        rp.fetchSize = fetchSize != null ? fetchSize : props.fetchSize;
        rp.executeInSameContext = executeInSameContext != null ? executeInSameContext : props.executeInSameContext;
        rp.scrollType = scrollType != null ? scrollType : props.scrollType;
        rp.concurrency = concurrency != null ? concurrency : props.concurrency;
        return rp;
    }

    public RuntimeProperties executeInSameContext(boolean executeInSameContext) {
        this.executeInSameContext = executeInSameContext;
        return this;
    }

    public RuntimeProperties scrollType(ScrollType scrollType) {
        this.scrollType = scrollType;
        return this;
    }

    public RuntimeProperties concurrency(Concurrency concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public Boolean getExecuteInSameContext() {
        return executeInSameContext;
    }

    public ScrollType getScrollType() {
        return scrollType;
    }

    public Concurrency getConcurrency() {
        return concurrency;
    }
}
