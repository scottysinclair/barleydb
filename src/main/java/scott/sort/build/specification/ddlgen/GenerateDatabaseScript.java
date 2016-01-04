package scott.sort.build.specification.ddlgen;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.sort.api.specification.constraint.PrimaryKeyConstraintSpec;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;

public abstract class GenerateDatabaseScript {

    protected boolean dropAndCreateConstraintsOnClean = false;

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
        sb.append('\n');
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            generateUniqueConstraints(entitySpec, sb);
        }
        return sb.toString();
    }

    public String generateCleanScript(DefinitionsSpec definitionsSpec) {
        StringBuilder sb = new StringBuilder();
        List<EntitySpec> entities = new LinkedList<>(definitionsSpec.getEntitySpecs());

        if (dropAndCreateConstraintsOnClean) {
            for (EntitySpec entitySpec: entities) {
                generateDropForeignKeyConstraints(entitySpec, sb);
            }
        }
        /*
         * Abstract entities don't have all of the relations in them
         * remove them.
         */
        Collections.sort(entities, new DependencyComparator());
        List<String> tableNames = toTableNames( entities );
        removeDuplicates(tableNames);
        for (String tableName: tableNames) {
            sb.append("\ndelete from ");
            sb.append(tableName);
            sb.append(';');
        }

        if (dropAndCreateConstraintsOnClean) {
            for (EntitySpec entitySpec: entities) {
                generateFkConstraints(entitySpec, sb);
            }
        }
        return sb.toString();
    }

    public String generateDropScript(DefinitionsSpec definitionsSpec) {
        StringBuilder sb = new StringBuilder();
        List<EntitySpec> entities = new LinkedList<>(definitionsSpec.getEntitySpecs());

        for (EntitySpec entitySpec: entities) {
            for (UniqueConstraintSpec spec: entitySpec.getUniqueConstraints()) {
                sb.append("\nalter table ");
                sb.append(entitySpec.getTableName());
                sb.append(" drop index ");
                sb.append(spec.getName());
                sb.append(';');
            }

            generateDropForeignKeyConstraints(entitySpec, sb);

            PrimaryKeyConstraintSpec spec = entitySpec.getPrimaryKeyConstraint();
            if (spec != null) {
                sb.append("\nalter table ");
                sb.append(entitySpec.getTableName());
                sb.append(" drop primary key  ");
                sb.append(spec.getName());
                sb.append(';');
            }
        }

        /*
         * Abstract entities don't have all of the relations in them
         * remove them.
         */
        Collections.sort(entities, new DependencyComparator());
        List<String> tableNames = toTableNames( entities );
        removeDuplicates(tableNames);
        for (String tableName: tableNames) {
            sb.append("\ndrop table ");
            sb.append(tableName);
            sb.append(";");
        }
        return sb.toString();
    }

    private void generateDropForeignKeyConstraints(EntitySpec entitySpec, StringBuilder sb) {
        for (ForeignKeyConstraintSpec spec: entitySpec.getForeignKeyConstraints()) {
            sb.append("\nalter table ");
            sb.append(entitySpec.getTableName());
            sb.append(" drop foreign key ");
            sb.append(spec.getName());
            sb.append(';');
        }
    }

    private List<String> toTableNames(List<EntitySpec> entitySpecs) {
        List<String> tableNames = new ArrayList<>(entitySpecs.size());
        for (EntitySpec es: entitySpecs) {
            tableNames.add( es.getTableName() );
        }
        return tableNames;

    }

    private void removeDuplicates(List<String> tableNames) {
        Set<String> namesSet = new HashSet<>();
        List<String> namesList = new ArrayList<String>();
        for (ListIterator<String> i = tableNames.listIterator(tableNames.size()); i.hasPrevious();) {
            String name = i.previous();
            if (namesSet.add( name )) {
                namesList.add(name);
            }
        }
        Collections.reverse(namesList);
        tableNames.clear();
        tableNames.addAll(namesList);
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

    private static class DepKey {
        private final EntitySpec from;
        private final EntitySpec to;
        public DepKey(EntitySpec from, EntitySpec to) {
            this.from = from;
            this.to = to;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            DepKey other = (DepKey) obj;
            if (from == null) {
                if (other.from != null) return false;
            } else if (!from.equals(other.from)) return false;
            if (to == null) {
                if (other.to != null) return false;
            } else if (!to.equals(other.to)) return false;
            return true;
        }
    }

    private static class DependencyComparator implements Comparator<EntitySpec> {

        private Map<DepKey,Boolean> cache = new HashMap<>();

        @Override
        public int compare(EntitySpec o1, EntitySpec o2) {
            if (o1 == o2) {
                return 0;
            }
            if (dependsOn(o1, o2)) {
                return -1;
            }
            else if (dependsOn(o2, o1)) {
                return 1;
            }
            //if there is no dependency when we put o1 first
            //returning 0 is supposed to mean that the o1 and o2 are identitcal
            return -1;
        }

        private boolean dependsOn(EntitySpec e1, EntitySpec e2) {
            Boolean value = cache.get(new DepKey(e1, e2));
            if (value != null) {
                return value;
            }
            for (EntitySpec dep: getDependentEntitySpecs(e1)) {
                if (dep == e2) {
                    cache.put(new DepKey(e1, e2), true);
                    return true;
                }
                if (dependsOn(dep, e2)) {
                    return true;
                }
            }
            cache.put(new DepKey(e1, e2), false);
            return false;
        }
        private Collection<EntitySpec> getDependentEntitySpecs(EntitySpec spec) {
            Collection<EntitySpec> result = new HashSet<EntitySpec>();
            for (NodeSpec nodeSpec: spec.getNodeSpecs()) {
                if (nodeSpec.getRelationSpec() != null && nodeSpec.getRelationSpec().isForeignKeyRelation()) {
                    if (nodeSpec.getRelationSpec().getEntitySpec() != spec) {
                        result.add( nodeSpec.getRelationSpec().getEntitySpec() );
                    }
                }
            }
            return result;
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

    private void generateUniqueConstraints(EntitySpec entitySpec, StringBuilder sb) {
        for (UniqueConstraintSpec spec: entitySpec.getUniqueConstraints()) {
            sb.append("\nalter table ");
            sb.append(entitySpec.getTableName());
            sb.append(" add constraint ");
            sb.append(spec.getName());
            sb.append(" unique (");
            for (NodeSpec nodeSpec: spec.getNodes()) {
                sb.append(nodeSpec.getColumnName());
                sb.append(',');
            }
            sb.setLength(sb.length()-1);
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

    protected abstract void generateColumnType(NodeSpec nodeSpec, StringBuilder sb);

    protected void generateLength(NodeSpec nodeSpec, StringBuilder sb) {
        sb.append('(');
        sb.append(nodeSpec.getLength());
        sb.append(')');
    }

}
