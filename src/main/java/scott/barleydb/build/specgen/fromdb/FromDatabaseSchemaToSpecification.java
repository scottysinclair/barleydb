package scott.barleydb.build.specgen.fromdb;

import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;
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

public class FromDatabaseSchemaToSpecification {

    private static Logger LOG = LoggerFactory.getLogger(FromDatabaseSchemaToSpecification.class);

    private final String namespace;
    private final SpecRegistry registry = new SpecRegistry();
    private final DefinitionsSpec spec;
    private final Set<String> prefixesToRemove = new HashSet();
    private final Map<Table, EntitySpec> entitySpecs = new HashMap<>();
    private final Map<Column, NodeSpec> nodeSpecs = new HashMap<>();
    private final Set<ProcessedFk> processedFks = new HashSet<>();

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

    public FromDatabaseSchemaToSpecification(String namespace) {
        this.namespace = namespace;
        spec = new DefinitionsSpec();
        spec.setNamespace( namespace );
        registry.add(spec);
    }

    public void removePrefix(String ...prefixes) {
        for (String prefix: prefixes) {
            prefixesToRemove.add( prefix.toLowerCase() );
        }
    }

    public SpecRegistry generateSpecification(DataSource dataSource) throws Exception {
        SchemaCrawlerOptions options = new SchemaCrawlerOptions();
        // Set what details are required in the schema - this affects the
        // time taken to crawl the schema
        options.setSchemaInfoLevel(SchemaInfoLevelBuilder.detailed());

        Catalog catalog = SchemaCrawlerUtility.getCatalog( dataSource.getConnection(),
                                                          options);

        firstPass(catalog);
        secondPass(catalog);
        return registry;
    }

    private void secondPass(Catalog catalog) {
        for (Schema schema: catalog.getSchemas()) {
            System.out.println(schema);
            for (Table table: catalog.getTables(schema)) {
                for (ForeignKey fk: table.getForeignKeys()) {
                    for (ForeignKeyColumnReference fkRef: fk.getColumnReferences()) {
                        Column fkCol = fkRef.getForeignKeyColumn();
                        Column pkCol = fkRef.getPrimaryKeyColumn();
                        Table pkTable = pkCol.getParent();

                        EntitySpec pkEspec = entitySpecs.get(pkTable);
                        EntitySpec fkEspec = entitySpecs.get(fkCol.getParent());
                        NodeSpec fkNSpec = nodeSpecs.get(fkCol);

                        if (!processedFks.add(new ProcessedFk(fkNSpec, pkEspec))) {
                            continue;
                        }

                        /*
                         *  Do the N:1 natual foreign key relation *
                         */
                        fkNSpec.setName( genRealtionNodeName(pkEspec) );
                        RelationSpec rspec = new RelationSpec();
                        rspec.setEntitySpec(pkEspec);
                        rspec.setJoinType(JoinTypeSpec.LEFT_OUTER_JOIN);
                        rspec.setType(RelationType.REFERS);
                        fkNSpec.setRelation(rspec);

                        LOG.debug("Added FK relation from {}.{} to {}", fkEspec.getClassName(), fkNSpec.getName(), pkEspec.getClassName());

                        /*
                         * do the opposite 1:N relation
                         */
                        //create the nodespec as there is no dbcolumn whch created a node for us
                        LOG.debug("Creating tomany node for N relation from {} to {}", pkEspec.getClassName(), fkEspec.getClassName());
                        NodeSpec toManyNodeSpec = new NodeSpec();
                        String nodeName = genRealtionNodeName(fkEspec);
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

    private String genRealtionNodeName(EntitySpec pkEspec) {
        int i = pkEspec.getClassName().lastIndexOf('.');
        String name = pkEspec.getClassName().substring(i+1, pkEspec.getClassName().length());
        char c = Character.toLowerCase( name.charAt(0) );
        String result = c + name.substring(1, name.length());
        return ensureFirstLetterIsLower( removePrefixes(result) );

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

    private void firstPass(Catalog catalog) {
        LOG.debug("First pass...");
        for (Schema schema: catalog.getSchemas()) {
          LOG.debug("Processing schema...");
          for (Table table: catalog.getTables(schema)) {
            LOG.debug("Processing table {}", table.getName());
            EntitySpec espec = toEntitySpec(table);
            entitySpecs.put(table, espec);
            spec.add( espec );
          }
        }

    }

    private EntitySpec toEntitySpec(Table table) {
        EntitySpec entitySpec = new EntitySpec();
        entitySpec.setTableName( table.getName());
        entitySpec.setClassName( generateClassName(table) );
        entitySpec.setAbstractEntity(false);
        entitySpec.setQueryClassName( generateQueryClassName(table));

        for (Column column: table.getColumns()) {
            LOG.debug("Processing column {}", column.getName());
            NodeSpec nodeSpec = toNodeSpec(entitySpec, column);
            entitySpec.add( nodeSpec );
        }
        return entitySpec;
    }

    private NodeSpec toNodeSpec(EntitySpec entitySpec, Column column) {
        NodeSpec nodeSpec = new NodeSpec();
        nodeSpec.setName( getNodeName( column ));
        nodeSpec.setColumnName( column.getName());
        nodeSpec.setJdbcType( getNodeType( column ));
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

    private static String getNodeName(Column column) {
        return toCamelCase(column.getName());
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

    private static JavaType getJavaType(JdbcType jdbcType) {
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
        default: throw new IllegalArgumentException("Unsupported JDBC type " + jdbcType);
        }
    }

    private static JdbcType getNodeType(Column column) {
        switch(column.getColumnDataType().getJavaSqlType().getJavaSqlType()) {
        case Types.VARCHAR: return JdbcType.VARCHAR;
        case Types.INTEGER: return JdbcType.INT;
        case Types.BIGINT: return JdbcType.BIGINT;
        case Types.CHAR: return JdbcType.CHAR;
        case Types.DATE: return JdbcType.DATE;
        case Types.TIMESTAMP: return JdbcType.TIMESTAMP;
        case Types.DECIMAL: return JdbcType.DECIMAL;
        default: throw new IllegalArgumentException("Unsupported column type " + column.getColumnDataType().getJavaSqlType());
        }
    }

    private String generateQueryClassName(Table table) {
        String ccName = "Q" + removePrefixes( toCamelCase(table.getName()) );
        return  namespace + ".query." + ccName;
    }

    private String generateClassName(Table table) {
        String ccName = toCamelCase(table.getName());
        ccName = Character.toUpperCase( ccName.charAt(0) ) + ccName.substring(1, ccName.length());
        return namespace + ".model." + removePrefixes( ccName );
    }


}
