package scott.barleydb.test;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
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

import javax.sql.DataSource;

import scott.barleydb.api.core.Environment;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.persist.Persister;

public class TestEntityContextServices extends JdbcEntityContextServices {

    public interface PersisterFactory {
        public Persister newPersister(Environment env, String namespace);
    }

    private PersisterFactory fac;

    public TestEntityContextServices(DataSource dataSource) {
        super(dataSource);
    }

    public void setPersisterFactory(PersisterFactory fac) {
        this.fac = fac;
    }

    @Override
    protected Persister newPersister(Environment env, String namespace) {
        if (fac != null) {
            return fac.newPersister(env, namespace);
        }
        return super.newPersister(env, namespace);
    }

}
