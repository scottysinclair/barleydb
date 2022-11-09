package scott.barleydb.build.specgen.fromdb;

import static java.util.Arrays.asList;

import java.sql.SQLException;

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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.IndexColumn;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.config.RelationType;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.JoinTypeSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.barleydb.api.specification.constraint.PrimaryKeyConstraintSpec;

/**
 * Generates a schema specification from database meta-data
 * @author scott
 *
 */
public class FromDatabaseSchemaToSpecification {

    private static Logger LOG = LoggerFactory.getLogger(FromDatabaseSchemaToSpecification.class);

  public interface ExclusionRules {
    boolean excludeTable(Table table);

    boolean excludeColumn(Column column);
  }

  public static ExclusionRules excludeTables(String... tableNames) {
     return new ExclusionRules() {
        @Override
        public boolean excludeTable(final Table table) {
           return Arrays.asList(tableNames).contains(table.getName());
        }

        @Override
        public boolean excludeColumn(final Column column) {
           return false;
        }
     };
  }

    private final String namespace;
    private final SpecRegistry registry = new SpecRegistry();
    private final DefinitionsSpec spec;
    private final Set<String> prefixesToRemove = new HashSet<>();
    private final Map<Table, EntitySpec> entitySpecs = new HashMap<>();
    private final Map<Column, NodeSpec> nodeSpecs = new HashMap<>();
    private final Set<ProcessedFk> processedFks = new HashSet<>();
    private final ExclusionRules exclusionRules;

    public FromDatabaseSchemaToSpecification(String namespace) {
      this(namespace, new ExclusionRules() {
        @Override
        public boolean excludeTable(Table tableName) {
          return false;
        }

        @Override
        public boolean excludeColumn(Column column) {
          return false;
        }
      });
    }
    public FromDatabaseSchemaToSpecification(String namespace, ExclusionRules exclusionRules) {
        this.namespace = namespace;
        this.exclusionRules = exclusionRules;
        spec = new DefinitionsSpec();
        spec.setNamespace( namespace );
        registry.add(spec);
    }

    public void removePrefix(String ...prefixes) {
        for (String prefix: prefixes) {
            prefixesToRemove.add( prefix.toLowerCase() );
        }
    }

    public SpecRegistry generateSpecification(DataSource dataSource, String schemaName) throws Exception {
        SchemaCrawlerOptions options = new SchemaCrawlerOptions();
        // Set what details are required in the schema - this affects the
        // time taken to crawl the schema
        options.setSchemaInfoLevel(SchemaInfoLevelBuilder.detailed());
        if (schemaName != null) {
          options.setSchemaInclusionRule(s -> s.equals( schemaName ));
        }

        Catalog catalog = loadCatalog(dataSource, options);

        firstPass(catalog);
        secondPass(catalog);
        postProcess(catalog);
        return registry;
    }

    protected Catalog loadCatalog(DataSource dataSource, SchemaCrawlerOptions options) throws SchemaCrawlerException, SQLException {
        return SchemaCrawlerUtility.getCatalog( dataSource.getConnection(),
                options);
    }

    /**
     * provides the oppertunity for further processing.
     * @param catalog
     */
    protected void postProcess(Catalog catalog ) {
    }


    private void createForeignKeyConstraint(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
        String keySpecName = createForeignKeyConstraintName( entitySpec, nodeSpec, relationSpec );
        EntitySpec toEntitySpec = relationSpec.getEntitySpec();

        Collection<NodeSpec> toPrimaryKey = toEntitySpec.getPrimaryKeyNodes(true);
        if (toPrimaryKey == null) {
            throw new IllegalStateException("Cannot create foreign key reference to entity " + toEntitySpec.getClassName() + " which  has no primary key");
        }
        ForeignKeyConstraintSpec spec = new ForeignKeyConstraintSpec(keySpecName, asList(nodeSpec), toEntitySpec, toPrimaryKey);
        entitySpec.add(spec);
    }

    protected String createForeignKeyConstraintName(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
        return "fk_" + entitySpec.getTableName() + "_" + nodeSpec.getColumnName();
    }

    private String incrementNodeName(String nodeName) {
        char c = nodeName.charAt( nodeName.length() - 1);
        if (Character.isDigit(c)) {
            int i = Integer.parseInt("" + c);
            i++;
            return nodeName.substring(0, nodeName.length() -  1) + i;
        }
        else {
            return nodeName + "1";
        }
    }

    /**
     * the name of the node if it is a FK pointing to another table
     * @param fkNode
     * @return
     */
    private String genRealtionNodeName(NodeSpec fkNode) {
        String columnName = fkNode.getColumnName();
        if (columnName.toLowerCase().endsWith("_id")) {
            columnName = columnName.substring(0, columnName.length() -  3);
        }
        return toCamelCase(columnName);
    }

    private String genRealtionNodeName(EntitySpec pkEspec, boolean plural) {
        int i = pkEspec.getClassName().lastIndexOf('.');
        String name = pkEspec.getClassName().substring(i+1, pkEspec.getClassName().length());
        char c = Character.toLowerCase( name.charAt(0) );
        String result = c + name.substring(1, name.length());
        result = ensureFirstLetterIsLower( removePrefixes(result) );
        return plural ? result + "s" : result;
    }

    private String ensureFirstLetterIsLower(String name) {
        char c = name.charAt(0);
        if (Character.isLowerCase(c)) {
            return name;
        }
        return Character.toLowerCase(c) + name.substring(1, name.length());
    }

    private String removePrefixes(String name) {
        String lcName = name.toLowerCase();
        for (String prefix: prefixesToRemove) {
            if (lcName.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    /**
     * create the entityspecs and nodespecs.
     * @param catalog
     */
    private void firstPass(Catalog catalog) {
        LOG.debug("First pass...");
        for (Schema schema: catalog.getSchemas()) {
          LOG.debug("Processing schema...");
          for (Table table: catalog.getTables(schema)) {
             if (exclusionRules.excludeTable(table)) {
              continue;
             }
            LOG.debug("Processing table {}", table.getName());
            EntitySpec espec = toEntitySpec(table);
            entitySpecs.put(table, espec);
            spec.add( espec );
          }
        }

    }

    /**
     * process the foreign key relations and constraints.
     */
    private void secondPass(Catalog catalog) {
        for (Schema schema: catalog.getSchemas()) {
            LOG.debug(schema.toString());
            for (Table table: catalog.getTables(schema)) {
                for (ForeignKey fk: table.getForeignKeys()) {
                    for (ForeignKeyColumnReference fkRef: fk.getColumnReferences()) {
                        Column fkCol = fkRef.getForeignKeyColumn();
                        Column pkCol = fkRef.getPrimaryKeyColumn();
                        Table pkTable = pkCol.getParent();

                        EntitySpec pkEspec = entitySpecs.get(pkTable);
                        EntitySpec fkEspec = entitySpecs.get(fkCol.getParent());
                        NodeSpec fkNSpec = nodeSpecs.get(fkCol);
                        if (pkEspec == null || fkEspec == null || fkNSpec == null) {
                          continue;
                        }

                        if (!processedFks.add(new ProcessedFk(fkNSpec, pkEspec))) {
                            continue;
                        }

                        /*
                         *  Do the N:1 natuarl foreign key relation *
                         */
                        fkNSpec.setName( genRealtionNodeName(fkNSpec) );
                        //java type is null, as the relation defines the type.
                        fkNSpec.setJavaType(null);
                        RelationSpec rspec = new RelationSpec();
                        rspec.setEntitySpec(pkEspec);
                        rspec.setJoinType(JoinTypeSpec.LEFT_OUTER_JOIN);
                        rspec.setType(RelationType.REFERS);
                        fkNSpec.setRelation(rspec);

                        LOG.debug("Added FK relation from {}.{} to {}", fkEspec.getClassName(), fkNSpec.getName(), pkEspec.getClassName());

                        createForeignKeyConstraint(fkEspec, fkNSpec, rspec);

                        /*
                         * do the opposite 1:N relation
                         */
                        //create the nodespec as there is no dbcolumn whch created a node for us
                        LOG.debug("Creating tomany node for N relation from {} to {}", pkEspec.getClassName(), fkEspec.getClassName());
                        NodeSpec toManyNodeSpec = new NodeSpec();
                        String nodeName = genRealtionNodeName(fkEspec, true);
                        while (pkEspec.getNodeSpec(nodeName) != null) {
                            nodeName = incrementNodeName(nodeName);
                        }
                        toManyNodeSpec.setName( nodeName );
                        toManyNodeSpec.setEntity(pkEspec);

                        rspec = new RelationSpec();
                        rspec.setBackReference(fkNSpec);
                        rspec.setEntitySpec(fkEspec);
                        rspec.setJoinType(JoinTypeSpec.LEFT_OUTER_JOIN);
                        rspec.setType(RelationType.REFERS);
                        toManyNodeSpec.setRelation(rspec);
                        pkEspec.add(toManyNodeSpec);
                    }
                }
            }
        }
    }


    protected EntitySpec toEntitySpec(Table table) {
        EntitySpec entitySpec = new EntitySpec();
        entitySpec.setTableName( table.getName());
        entitySpec.setClassName( generateClassName(table) );
        entitySpec.setAbstractEntity(false);
        entitySpec.setQueryClassName( generateQueryClassName(table) );
        entitySpec.setDtoClassName( generateDtoClassName(table) );

        for (Column column: table.getColumns()) {
            if (exclusionRules.excludeColumn( column )) {
              continue;
            }
            LOG.debug("Processing column {}", column.getName());
            NodeSpec nodeSpec = toNodeSpec(entitySpec, table, column);
            entitySpec.add( nodeSpec );

            //set PK contraints
            Collection<NodeSpec> key = new LinkedList<>();
            for (NodeSpec ns: entitySpec.getNodeSpecs()) {
                if (ns.isPrimaryKey()) {
                    key.add(ns);
                }
            }
            if (!key.isEmpty()) {
                createPrimaryKeyConstraint(entitySpec, key);
            }

        }
        return entitySpec;
    }

    protected NodeSpec toNodeSpec(EntitySpec entitySpec, Table table, Column column) {
        NodeSpec nodeSpec = new NodeSpec();
        nodeSpec.setName( getNodeName( column ));
        if (isPrimaryKey(table, column)) {
            nodeSpec.setPrimaryKey(true);
        }
        nodeSpec.setColumnName( column.getName());
        nodeSpec.setJdbcType( getNodeJdbcType( column ));
        nodeSpec.setJavaType( getJavaType( nodeSpec.getJdbcType() ));
        nodeSpec.setNullable( column.isNullable() ? Nullable.NULL : Nullable.NOT_NULL);
        if (nodeSpec.getJavaType() == JavaType.STRING) {
            nodeSpec.setLength( column.getSize() );
        }
        else if (nodeSpec.getJavaType() == JavaType.BIGDECIMAL) {
            nodeSpec.setPrecision( column.getSize() );
            nodeSpec.setScale( column.getDecimalDigits());
        }
        nodeSpec.setEntity(entitySpec);
        nodeSpecs.put(column, nodeSpec);
        return nodeSpec;
    }

  protected boolean isPrimaryKey(Table table, Column column) {
    if (table.getPrimaryKey() != null) {
      for (IndexColumn col : table.getPrimaryKey().getColumns()) {
        if (col.getName().equalsIgnoreCase(column.getName())) {
          return true;
        }
      }
    } else {
      LOG.warn("No primary key constraints for {}", table.getName());
    }
    return false;
  }

  protected static String getNodeName(Column column) {
    return toCamelCase(stripBadChars( column.getName() ));
  }

  protected static String stripBadChars(String name) {
    return name.replaceAll("\"", "");
  }

    private static String toCamelCase(String nameString) {
        String name = nameString.toLowerCase();
        int i = name.indexOf('_');
        while(i != -1) {
            String start = name.substring(0,  i);
            String end = "";
            if (i+2 < name.length()) {
                end = Character.toUpperCase( name.charAt(i+1) ) + name.substring(i+2, name.length());
            }
            name = start + end;
            i = name.indexOf('_');
        }
        return name;
    }

    protected JavaType getJavaType(JdbcType jdbcType) {
        switch(jdbcType) {
        case INT: return JavaType.INTEGER;
        case DECIMAL: return JavaType.BIGDECIMAL;
        case BIGINT: return JavaType.LONG;
        case CHAR: return JavaType.STRING;
        case NVARCHAR: return JavaType.STRING;
        case VARCHAR: return JavaType.STRING;
        case DATE: return JavaType.UTIL_DATE;
        case DATETIME: return JavaType.UTIL_DATE;
        case TIMESTAMP: return JavaType.UTIL_DATE;
        case BLOB: return JavaType.BYTE_ARRAY;
        case CLOB: return JavaType.STRING;
        case SMALLINT: return JavaType.INTEGER;
        case UUID: return JavaType.UUID;
        default: throw new IllegalArgumentException("Unsupported JDBC type " + jdbcType);
        }
    }

    protected JdbcType getNodeJdbcType(Column column) {
        switch(column.getColumnDataType().getJavaSqlType().getJavaSqlType()) {
        case Types.VARCHAR: return JdbcType.VARCHAR;
        case Types.INTEGER: return JdbcType.INT;
        case Types.BIGINT: return JdbcType.BIGINT;
        case Types.CHAR: return JdbcType.CHAR;
        case Types.DATE: return JdbcType.DATE;
        case Types.TIMESTAMP: return JdbcType.TIMESTAMP;
        case Types.DECIMAL: return JdbcType.DECIMAL;
        case Types.NUMERIC: return JdbcType.DECIMAL;
        case Types.SMALLINT: return JdbcType.SMALLINT;
        case Types.BIT: return JdbcType.SMALLINT;
        case Types.BLOB: return JdbcType.BLOB;
        case Types.CLOB: return JdbcType.CLOB;
        case 1111: return JdbcType.UUID;
        case 2147483647: {
          JdbcType jt = getJdbcTypeForUnknownJavaSqlType(column);
          if (jt != null) {
            return jt;
          }
        }
        }
       throw new IllegalArgumentException("Unsupported column type " + column.getColumnDataType().getJavaSqlType() + " (" + column.getColumnDataType().getJavaSqlType().getJavaSqlType() + ")");
    }

    protected JdbcType getJdbcTypeForUnknownJavaSqlType(Column column) {
      /*
       * unknown type
       */
      if (column.getColumnDataType().getName().contains("TIMESTAMP")) {
          return JdbcType.TIMESTAMP;
      }
      return null;
    }

    protected String generateQueryClassName(Table table) {
        String ccName = "Q" + removePrefixes( toCamelCase(table.getName().replace("\"", "")) );
        return  namespace + ".query." + ccName;
    }

    protected String generateDtoClassName(Table table) {
      String ccName = toCamelCase(table.getName().replace("\"", ""));
      ccName = Character.toUpperCase( ccName.charAt(0) ) + ccName.substring(1, ccName.length());
      return namespace + ".dto." + removePrefixes( ccName )+ "Dto";
  }

    protected String generateClassName(Table table) {
        String ccName = toCamelCase(table.getName().replace("\"", ""));
        ccName = Character.toUpperCase( ccName.charAt(0) ) + ccName.substring(1, ccName.length());
        return namespace + ".model." + removePrefixes( ccName );
    }

    private void createPrimaryKeyConstraint(EntitySpec entitySpec, Collection<NodeSpec> key) {
        String keySpecName = createPrimaryKeyConstraintName( entitySpec, key );
        PrimaryKeyConstraintSpec pkSpec = new PrimaryKeyConstraintSpec(keySpecName, key);
        entitySpec.setPrimaryKeyConstraint( pkSpec );
    }

    protected String createPrimaryKeyConstraintName(EntitySpec entitySpec, Collection<NodeSpec> key) {
        return "pk_" + entitySpec.getTableName();
    }

    private static class ProcessedFk {
        private final NodeSpec from;
        private final EntitySpec to;
        public ProcessedFk(NodeSpec from, EntitySpec to) {
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
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ProcessedFk other = (ProcessedFk) obj;
            if (from == null) {
                if (other.from != null)
                    return false;
            } else if (!from.equals(other.from))
                return false;
            if (to == null) {
                if (other.to != null)
                    return false;
            } else if (!to.equals(other.to))
                return false;
            return true;
        }
    }

}
