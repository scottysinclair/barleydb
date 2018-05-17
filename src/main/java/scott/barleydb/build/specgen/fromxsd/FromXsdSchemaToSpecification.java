package scott.barleydb.build.specgen.fromxsd;

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

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import scott.barleydb.api.config.RelationType;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.JoinTypeSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.barleydb.xsd.XsdComplexType;
import scott.barleydb.xsd.XsdCoreType;
import scott.barleydb.xsd.XsdDefinition;
import scott.barleydb.xsd.XsdElement;
import scott.barleydb.xsd.XsdNode;
import scott.barleydb.xsd.XsdSimpleType;
import scott.barleydb.xsd.XsdType;
import scott.barleydb.xsd.exception.XsdDefinitionException;

public class FromXsdSchemaToSpecification {

  private static Logger LOG = LoggerFactory.getLogger(FromXsdSchemaToSpecification.class);

  private final String namespace;
  private final DefinitionsSpec spec;
  private final SpecRegistry registry = new SpecRegistry();
  private final Collection<XsdType> allTypes = new LinkedList<>();
  private final Map<XsdType, EntitySpec> entitySpecs = new LinkedHashMap<>();
  private final Map<String, EntitySpec> entitySpecsByTypeName = new LinkedHashMap<>();

  public FromXsdSchemaToSpecification(String namespace) {
    this.namespace = namespace;
    this.spec = new DefinitionsSpec();
    spec.setNamespace(namespace);
    registry.add(spec);
  }

  public SpecRegistry generateSpecification(XsdDefinition xsdDefinition) throws Exception {
    Collection<XsdElement> allRootElements = xsdDefinition.getChildTargetElements();
    allTypes.addAll(extractTypes(allRootElements));

    firstPass();
    secondPass();
    postProcess();

    return registry;
  }

  private Collection<XsdType> extractTypes(Collection<XsdElement> allRootElements) throws XsdDefinitionException {
    Map<Node, XsdType> processedTypes = new LinkedHashMap<>();
    extractTypes(allRootElements, processedTypes);
    return processedTypes.values();
  }

  private void extractTypes(Collection<XsdElement> elements, Map<Node, XsdType> processedTypes)
      throws XsdDefinitionException {
    for (XsdElement el : elements) {
      LOG.debug("Extracing type from element {}", el.getElementName());
      XsdType type = el.getType();
      if (!(type instanceof XsdCoreType)) {
        if (!processedTypes.containsKey(type.getDomNode())) {
          processedTypes.putIfAbsent(type.getDomNode(), type);
          extractTypes(el.getChildTargetElements(), processedTypes);
        }
      }
    }
  }

  /**
   * create the entityspecs and nodespecs.
   *
   * @throws XsdDefinitionException
   */

  private void firstPass() throws XsdDefinitionException {
    LOG.debug("First pass...");
    for (XsdType type : allTypes) {
      LOG.debug("Processing type {}", getEntityClassName(type));
      EntitySpec entitySpec = toEntitySpec(type);
      entitySpecs.put(type, entitySpec);
      entitySpecsByTypeName.put(type.getTypeLocalName(), entitySpec);
      spec.add(entitySpec);
    }

  }

  private EntitySpec toEntitySpec(XsdType type) throws XsdDefinitionException {
    EntitySpec entitySpec = new EntitySpec();
    entitySpec.setTableName(getEntityTableName(type));
    entitySpec.setClassName(getEntityClassName(type));
    entitySpec.setDtoClassName(getEntityDtoClassName(type));
    entitySpec.setQueryClassName(getEntityQueryClassName(type));

    for (XsdElement el : type.getChildTargetElements()) {
      if (el.getType() instanceof XsdCoreType || el.getType() instanceof XsdSimpleType) {
        NodeSpec nodeSpec = new NodeSpec();
        nodeSpec.setEntity(entitySpec);
        nodeSpec.setName(getNodeName(el));
        nodeSpec.setColumnName(getNodeColumnName(el));
        nodeSpec.setEnumSpec(getNodeEnumSpec(el));
        nodeSpec.setFixedValue(getNodeFixedValue(el));
        nodeSpec.setJavaType(getNodeJavaType(el));
        nodeSpec.setJdbcType(getNodejdbcType(el));
        nodeSpec.setLength(getNodeLength(el));
        nodeSpec.setNullable(getNodeNullable(el));
        nodeSpec.setPrecision(getNodePrecision(el));
        nodeSpec.setScale(getNodeScale(el));
        entitySpec.add(nodeSpec);
        LOG.debug("Adding nodespec {}", nodeSpec);
      }
    }
    if (entitySpec.getNodeSpec("id") == null) {
      NodeSpec pk = new NodeSpec();
      pk.setEntity(entitySpec);
      pk.setName("id");
      pk.setColumnName("ID");
      pk.setJavaType(JavaType.LONG);
      pk.setJdbcType(JdbcType.BIGINT);
      pk.setNullable(Nullable.NOT_NULL);
      pk.setPrimaryKey(true);
      entitySpec.add(pk);
    } else {
      entitySpec.getNodeSpec("id").setPrimaryKey(true);
    }

    return entitySpec;
  }

  private String getNodeName(XsdElement el) throws XsdDefinitionException {
    String name =  el.getElementName();
    if (name.equals("return") || name.equals("volatile")) {
      name += "Value";
    }
    return name;
  }

  private String getNodeColumnName(XsdElement el) throws XsdDefinitionException {
    return el.getElementName();
  }

  private EnumSpec getNodeEnumSpec(XsdElement el) {
    return null;
  }

  private Object getNodeFixedValue(XsdElement el) {
    return null;
  }

  private JavaType getNodeJavaType(XsdElement el) throws XsdDefinitionException {
    XsdType type = el.getType();
    switch (type.getTypeLocalName()) {
    case "String":
      return JavaType.STRING;
    case "int":
      return JavaType.INTEGER;
    case "boolean":
      return JavaType.BOOLEAN;
    case "decimal":
      return JavaType.BIGDECIMAL;
    case "dateTime":
      return JavaType.SQL_DATE;
    case "time":
      return JavaType.SQL_DATE;
    case "nonNegativeInteger":
      return JavaType.INTEGER;
    case "PositiveInteger":
      return JavaType.INTEGER;
    default: {
      LOG.warn("Defaulting " + type.getTypeLocalName() + " to String");
      return JavaType.STRING;
    }
    }
  }

  private JdbcType getNodejdbcType(XsdElement el) throws XsdDefinitionException {
    JavaType javaType = getNodeJavaType(el);
    switch (javaType) {
    case STRING:
      return JdbcType.VARCHAR;
    case INTEGER:
      return JdbcType.INT;
    case BOOLEAN:
      return JdbcType.SMALLINT;
    case SQL_DATE:
      return JdbcType.DATETIME;
    case BIGDECIMAL:
      return JdbcType.DECIMAL;
    default:
      throw new IllegalStateException("Unknown type " + javaType);
    }
  }

  private Integer getNodeLength(XsdElement el) throws XsdDefinitionException {
    JavaType javaType = getNodeJavaType(el);
    if (javaType == JavaType.STRING) {
      return 100;
    } else if (javaType == JavaType.BIGDECIMAL) {
      return 9;
    }
    return null;
  }

  private Nullable getNodeNullable(XsdElement el) throws XsdDefinitionException {
    return el.getMinOccurs() == 0 ? Nullable.NULL : Nullable.NOT_NULL;
  }

  private Integer getNodePrecision(XsdElement el) throws XsdDefinitionException {
    JavaType javaType = getNodeJavaType(el);
    if (javaType == JavaType.BIGDECIMAL) {
      return 4;
    }
    return null;
  }

  private Integer getNodeScale(XsdElement el) throws XsdDefinitionException {
    JavaType javaType = getNodeJavaType(el);
    if (javaType == JavaType.BIGDECIMAL) {
      return 9;
    }
    return null;
  }

  private String getEntityQueryClassName(XsdType type) throws XsdDefinitionException {
    return namespace + ".query.Q" + getEntityName(type);
  }

  private String getEntityName(XsdType type) throws XsdDefinitionException {
    if (type.getTypeLocalName() != null && type.getTypeLocalName().length() > 0) {
      return type.getTypeLocalName();
    }
    XsdNode parent = type.getParent();
    if (parent instanceof XsdElement) {
      return ((XsdElement) parent).getElementName();
    }
    throw new IllegalStateException("Cannot get type name for type " + type);
  }

  private String getEntityDtoClassName(XsdType type) throws XsdDefinitionException {
    return namespace + ".dto." + getEntityName(type) + "Dto";
  }

  private String getEntityClassName(XsdType type) throws XsdDefinitionException {
    return namespace + ".model." + getEntityName(type);
  }

  private String getEntityTableName(XsdType type) throws XsdDefinitionException {
    return getEntityName(type);
  }

  /**
   * process the foreign key relations and constraints.
   *
   * @throws XsdDefinitionException
   */
  private void secondPass() throws XsdDefinitionException {
    for (Map.Entry<XsdType, EntitySpec> entry : entitySpecs.entrySet()) {
      XsdType type = entry.getKey();
      EntitySpec entitySpec = entry.getValue();
      for (XsdElement el : type.getChildTargetElements()) {
        if (el.getMaxOccurs() != 1) {
          continue;
        }
        if (el.getType() instanceof XsdComplexType) {

          EntitySpec fkEntity = entitySpecsByTypeName.get(el.getType().getTypeLocalName());
          Objects.requireNonNull(fkEntity, "Cannot find entity for type {}" + el.getType());
          // normal FK relation
          NodeSpec fkNodeSpec = new NodeSpec();
          fkNodeSpec.setEntity(entitySpec);
          fkNodeSpec.setName(getNodeName(el));
          fkNodeSpec.setColumnName(getFkColumnName(el));
          RelationSpec rspec = new RelationSpec();
          rspec.setEntitySpec(fkEntity);
          rspec.setJoinType(JoinTypeSpec.LEFT_OUTER_JOIN);
          rspec.setType(RelationType.REFERS);
          fkNodeSpec.setRelation(rspec);
          entitySpec.add(fkNodeSpec);

          createForeignKeyConstraint(entitySpec, fkNodeSpec, rspec);
          LOG.debug("Added FK relation from {}.{} to {}", entitySpec.getClassName(), fkNodeSpec.getName(),
              fkEntity.getClassName());
        }
      }
    }
  }

  private String getFkColumnName(XsdElement el) throws XsdDefinitionException {
    return getNodeName(el);
  }

  private void postProcess() {
    // TODO Auto-generated method stub

  }

  private void createForeignKeyConstraint(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
    String keySpecName = createForeignKeyConstraintName(entitySpec, nodeSpec, relationSpec);
    EntitySpec toEntitySpec = relationSpec.getEntitySpec();

    Collection<NodeSpec> toPrimaryKey = toEntitySpec.getPrimaryKeyNodes(true);
    if (toPrimaryKey == null) {
      throw new IllegalStateException("Cannot create foreign key reference to entity " + toEntitySpec.getClassName()
          + " which  has no primary key");
    }
    ForeignKeyConstraintSpec spec = new ForeignKeyConstraintSpec(keySpecName, asList(nodeSpec), toEntitySpec,
        toPrimaryKey);
    entitySpec.add(spec);
  }

  protected String createForeignKeyConstraintName(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
    return "fk_" + entitySpec.getTableName() + "_" + relationSpec.getEntitySpec().getTableName();
  }

  private String genRealtionNodeName(EntitySpec pkEspec) {
    int i = pkEspec.getClassName().lastIndexOf('.');
    String name = pkEspec.getClassName().substring(i + 1, pkEspec.getClassName().length());
    char c = Character.toLowerCase(name.charAt(0));
    String result = c + name.substring(1, name.length());
    return ensureFirstLetterIsLower(result);
  }

  private String ensureFirstLetterIsLower(String name) {
    char c = name.charAt(0);
    if (Character.isLowerCase(c)) {
      return name;
    }
    return Character.toLowerCase(c) + name.substring(1, name.length());
  }

  private String incrementNodeName(String nodeName) {
    char c = nodeName.charAt(nodeName.length() - 1);
    if (Character.isDigit(c)) {
      int i = Integer.parseInt("" + c);
      i++;
      return nodeName.substring(0, nodeName.length() - 1) + i;
    } else {
      return nodeName + "1";
    }
  }

}
