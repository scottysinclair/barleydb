package scott.barleydb.build.specification.ddlgen;

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

import java.util.Objects;

import scott.barleydb.api.specification.NodeSpec;

public class GeneratePostgreSQLDatabaseScript extends GenerateDatabaseScript {

    @Override
    protected void generateColumnType(NodeSpec nodeSpec, StringBuilder sb) {
        Objects.requireNonNull(nodeSpec.getJdbcType(), "JDBC type should not be null for " + nodeSpec);
        switch (nodeSpec.getJdbcType()) {
            case BIGINT:
                sb.append("BIGINT");
                break;
            case INT:
                sb.append("INTEGER");
                break;
            case NVARCHAR:
                sb.append("NATIONAL CHAR VARYING");
                generateLength(nodeSpec, sb);
                break;
            case TIMESTAMP:
                sb.append("TIMESTAMP");
                break;
            case DATE:
                sb.append("DATE");
                break;
            case DATETIME:
                sb.append("DATETIME");
                break;
            case DECIMAL:
                sb.append("DECIMAL");
                generatePrecisionAndScale(nodeSpec, sb);
                break;
            case VARCHAR:
                sb.append("VARCHAR");
                generateLength(nodeSpec, sb);
                break;
            case CHAR:
                sb.append("CHAR");
                generateLength(nodeSpec, sb);
                break;
            case BLOB:
                sb.append("MEDIUMBLOB");
                break;
            default:
                throw new IllegalStateException("Invalid JDBC type: " + nodeSpec.getJdbcType());
        }
    }

    private void generatePrecisionAndScale(NodeSpec nodeSpec, StringBuilder sb) {
        sb.append('(');
        sb.append(nodeSpec.getPrecision());
        sb.append(',');
        sb.append(nodeSpec.getScale());
        sb.append(')');
    }

}
