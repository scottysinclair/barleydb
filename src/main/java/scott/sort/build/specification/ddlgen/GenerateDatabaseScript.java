package scott.sort.build.specification.ddlgen;

import java.util.Objects;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.sort.api.specification.constraint.PrimaryKeyConstraintSpec;

public class GenerateDatabaseScript {

    public String generateScript(DefinitionsSpec definitionsSpec) {
        StringBuilder sb = new StringBuilder();
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            generateCreateTable(entitySpec, sb);
        }
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            generatePkConstraint(entitySpec, sb);
        }
        sb.append('\n');
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            generateFkConstraints(entitySpec, sb);
        }
        return sb.toString();
    }

    private void generateCreateTable(EntitySpec entitySpec, StringBuilder sb) {
        if (entitySpec.getParentEntity() != null) {
            if (entitySpec.getTableName().equals(entitySpec.getParentEntity().getTableName())) {
                return;
            }
        }
        sb.append("\ncreate table ");
        sb.append(entitySpec.getTableName());
        sb.append(" (");
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getColumnName() != null) {
                sb.append("\n  ");
                generateCreateColumn(nodeSpec, sb);
                sb.append(",");
            }
        }
        sb.setLength(sb.length()-1);
        sb.append("\n);\n");
    }

    private void generatePkConstraint(EntitySpec entitySpec, StringBuilder sb) {
        PrimaryKeyConstraintSpec pkSpec = entitySpec.getPrimaryKeyConstraint();
        if (pkSpec != null) {
            if (pkSpec.getNodes().size() != 1) {
                throw new IllegalStateException("Only PKs of 1 column are currently supported: " + pkSpec);
            }
            sb.append("\nalter table ");
            sb.append(entitySpec.getTableName());
            sb.append(" add constraint ");
            sb.append(pkSpec.getName());
            sb.append(" primary key (");
            sb.append(pkSpec.getNodes().iterator().next().getColumnName());
            sb.append(");");
        }
    }

    private void generateFkConstraints(EntitySpec entitySpec, StringBuilder sb) {
        for (ForeignKeyConstraintSpec spec: entitySpec.getForeignKeyConstraints()) {
            if (spec.getFromKey().size() != 1) {
                throw new IllegalStateException("Only FKs of 1 column are currently supported: " + spec);
            }
            if (spec.getToKey().size() != 1) {
                throw new IllegalStateException("Only FKs of 1 column are currently supported: " + spec);
            }
            sb.append("\nalter table ");
            sb.append(entitySpec.getTableName());
            sb.append(" add constraint ");
            sb.append(spec.getName());
            sb.append(" foreign key (");
            sb.append(spec.getFromKey().iterator().next().getColumnName());
            sb.append(") references ");
            NodeSpec toKey = spec.getToKey().iterator().next();
            sb.append(toKey.getEntity().getTableName());
            sb.append('(');
            sb.append(toKey.getColumnName());
            sb.append(");");
        }
    }


    private void generateCreateColumn(NodeSpec nodeSpec, StringBuilder sb) {
        sb.append(nodeSpec.getColumnName());
        sb.append(' ');
        generateColumnType(nodeSpec, sb);
        sb.append(' ');
        Objects.requireNonNull(nodeSpec.getNullable(), "Nullable should not be null for " + nodeSpec);
        switch (nodeSpec.getNullable()) {
            case NOT_NULL:
                sb.append("NOT NULL");
                break;
            case NULL:
                sb.append("NULL");
                break;
            default:
                throw new IllegalStateException("Invalid Nullable value: " + nodeSpec.getNullable());
        }
    }

    private void generateColumnType(NodeSpec nodeSpec, StringBuilder sb) {
        Objects.requireNonNull(nodeSpec.getJdbcType(), "JDBC type should not be null for " + nodeSpec);
        switch (nodeSpec.getJdbcType()) {
            case BIGINT:
                sb.append("BIGINT");
                break;
//            case BLOB:
//                break;
//            case CLOB:
//                break;
//            case DATE:
//                break;
//            case DECIMAL:
//                break;
            case INT:
                sb.append("INTEGER");
                break;
            case NVARCHAR:
                sb.append("NVARCHAR");
                generateLength(nodeSpec, sb);
                break;
            case TIMESTAMP:
                sb.append("TIMESTAMP");
                break;
            case VARCHAR:
                sb.append("VARCHAR");
                generateLength(nodeSpec, sb);
                break;
            default:
                throw new IllegalStateException("Invalid JDBC type: " + nodeSpec.getJdbcType());
        }
    }

    private void generateLength(NodeSpec nodeSpec, StringBuilder sb) {
        sb.append('(');
        sb.append(nodeSpec.getLength());
        sb.append(')');
    }

}
