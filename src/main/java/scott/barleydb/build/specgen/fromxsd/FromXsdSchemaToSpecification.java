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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import scott.barleydb.api.specification.KeyGenSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.barleydb.api.specification.constraint.PrimaryKeyConstraintSpec;
import scott.barleydb.xsd.XsdAttribute;
import scott.barleydb.xsd.XsdComplexType;
import scott.barleydb.xsd.XsdCoreType;
import scott.barleydb.xsd.XsdDefinition;
import scott.barleydb.xsd.XsdElement;
import scott.barleydb.xsd.XsdNode;
import scott.barleydb.xsd.XsdType;
import scott.barleydb.xsd.exception.XsdDefinitionException;

public class FromXsdSchemaToSpecification implements EntitySpecXsdLookup {

  private static Logger LOG = LoggerFactory.getLogger(FromXsdSchemaToSpecification.class);

  private final String namespace;
  private final DefinitionsSpec spec;
  private final SpecRegistry registry = new SpecRegistry();
  private final Collection<XsdType> allTypes = new LinkedList<>();
  private final Map<XsdType, EntitySpec> entitySpecs = new LinkedHashMap<>();
  private final Map<String, EntitySpec> entitySpecsByTypeName = new LinkedHashMap<>();
  private final Map<String,EntitySpec> xsdPathToEntitySpec = new HashMap<>();
  private final Map<String,NodeSpec> xsdPathToNodeSpec = new HashMap<>();
  private final String prefix;

  public FromXsdSchemaToSpecification(String namespace, String prefix) {
    this.namespace = namespace;
    this.spec = new DefinitionsSpec();
    this.prefix = prefix;
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

  @Override
  public EntitySpec getRequiredEntitySpecFor(XsdType xsdType) throws XsdDefinitionException {
      String xsdPath = getXsdPath(xsdType);
      EntitySpec spec = xsdPathToEntitySpec.get( xsdPath  );
      if (spec == null) {
        throw new IllegalStateException("Could not find entity spec for xsdPath " + xsdPath);
      }
      return spec;
  }

  @Override
  public NodeSpec getRequiredNodeSpecFor(XsdType xsdType, XsdElement xsdElement) throws XsdDefinitionException {
    String xsdPath = getXsdPath(xsdType, xsdElement);
    NodeSpec ns = xsdPathToNodeSpec.get(xsdPath);
    if (ns == null) {
      throw new IllegalStateException("Could not find node spec for element xsdPath " + xsdPath);
    }
    return ns;
  }

  @Override
  public NodeSpec getRequiredNodeSpecFor(XsdType xsdType, XsdAttribute xsdAttribute) throws XsdDefinitionException {
    String xsdPath = getXsdPath(xsdType, xsdAttribute);
    NodeSpec ns = xsdPathToNodeSpec.get(xsdPath);
    if (ns == null) {
      throw new IllegalStateException("Could not find node spec for attribute xsdPath " + xsdPath);
    }
    return ns;
  }

  private Collection<XsdType> extractTypes(Collection<XsdElement> allRootElements) throws XsdDefinitionException {
    Map<Node, XsdType> processedTypes = new LinkedHashMap<>();
    extractTypes(allRootElements, processedTypes);
    return processedTypes.values();
  }

  private void createPrimaryKeyConstraint(EntitySpec entitySpec, Collection<NodeSpec> key) {
    String keySpecName = createPrimaryKeyConstraintName( entitySpec, key );
    PrimaryKeyConstraintSpec pkSpec = new PrimaryKeyConstraintSpec(keySpecName, key);
    entitySpec.setPrimaryKeyConstraint( pkSpec );
  }

  protected String createPrimaryKeyConstraintName(EntitySpec entitySpec, Collection<NodeSpec> key) {
    return "pk_" + entitySpec.getTableName();
  }

  private void extractTypes(Collection<XsdElement> elements, Map<Node, XsdType> processedTypes)
      throws XsdDefinitionException {
    for (XsdElement el : elements) {
      XsdType type = el.getType();
      if (!(type instanceof XsdCoreType)) {
        if (!processedTypes.containsKey(type.getDomNode())) {
          LOG.debug("Extracting type from element {}", el.getElementName());
          processedTypes.put(type.getDomNode(), type);
          extractTypes(el.getChildTargetElements(), processedTypes);
        }
      }
      else {
        LOG.debug("Ignoring type {}", el.getElementName());
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
      if (shouldBeAnEntity(type)) {
        String xsdPath = getXsdPath(type);
        LOG.debug("Processing type {} with xsdPath {}", getEntityClassName(type), xsdPath);
        EntitySpec entitySpec = toEntitySpec(type);
        entitySpecs.put(type, entitySpec);
        entitySpecsByTypeName.put(type.getTypeLocalName(), entitySpec);
        if (xsdPathToEntitySpec.put(xsdPath, entitySpec) != null) {
          throw new IllegalStateException("XSD Path " + getXsdPath(type) + "already exists, cannot register " + entitySpec.getClassName());
        }
        spec.add(entitySpec);
      }
      else {
        LOG.debug("{} should not be an entity", type.getTypeLocalName());
      }
    }

  }

  private String getXsdPath(XsdType type) throws XsdDefinitionException {
    if (type.getTypeLocalName().length() > 0) {
      return getXsdPath(type.getParent()) + "/" + type.getTypeLocalName();
    }
    else {
      return getXsdPath(type.getParent()) + "/" + type.getDomNode().getNodeName();
    }
  }

  private String getXsdPath(XsdElement element) throws XsdDefinitionException {
    return getXsdPath(element.getParent()) + "/" + element.getElementName();
  }

  private String getXsdPath(XsdType type, XsdAttribute attr) throws XsdDefinitionException {
    return getXsdPath(type) + "/@" + attr.getName();
  }

  private String getXsdPath(XsdType type, XsdElement element) throws XsdDefinitionException {
    return getXsdPath(type) + "/" + element.getElementName();
  }

  private String getXsdPath(XsdNode xsdNode) throws XsdDefinitionException {
    if (xsdNode instanceof XsdElement) {
        return getXsdPath((XsdElement)xsdNode);
    }
    if (xsdNode instanceof XsdDefinition) {
      return ((XsdDefinition) xsdNode).getTargetNamespace();
    }
    if (xsdNode instanceof XsdType) {
      return getXsdPath((XsdType) xsdNode);
    }
    return getXsdPath(xsdNode.getParent()) + "/" + xsdNode.getDomNode().getNodeName();
  }


  protected boolean shouldBeAnEntity(XsdType type) throws XsdDefinitionException  {
    if (!typeHasMoreThanOneElementOrAttribute(type)) {
      LOG.warn("Type " + getEntityName(type) + " has less than 2 children (including attributes)");
      return false;
    }
    return true;
  }

  private EntitySpec toEntitySpec(XsdType type) throws XsdDefinitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Converting type {} to entity spec with path {}", type.getTypeLocalName(), getXsdPath(type));
    }
    EntitySpec entitySpec = new EntitySpec();
    entitySpec.setTableName(getEntityTableName(type));
    entitySpec.setClassName(getEntityClassName(type));
    entitySpec.setDtoClassName(getEntityDtoClassName(type));
    entitySpec.setQueryClassName(getEntityQueryClassName(type));

    for (XsdAttribute attr: type.getAttributes()) {
      String specPath = getXsdPath(type, attr);
      LOG.trace("Converting attribute {} to node spec with path {}", attr.getName(), specPath);
      NodeSpec nodeSpec = new NodeSpec();
      nodeSpec.setEntity(entitySpec);
      nodeSpec.setName(getNodeName(attr));
      nodeSpec.setColumnName(getNodeColumnName(attr));
      nodeSpec.setEnumSpec(getNodeEnumSpec(attr));
      nodeSpec.setFixedValue(getNodeFixedValue(attr));
      nodeSpec.setJavaType(getNodeJavaType(attr));
      nodeSpec.setJdbcType(getNodejdbcType(attr));
      nodeSpec.setLength(getNodeLength(attr));
      nodeSpec.setNullable(getNodeNullable(attr));
      nodeSpec.setPrecision(getNodePrecision(attr));
      nodeSpec.setScale(getNodeScale(attr));
      entitySpec.add(nodeSpec);
      if (xsdPathToNodeSpec.put(specPath, nodeSpec) != null) {
        throw new IllegalStateException("Duplicate xsd path for node spec: " + nodeSpec + ", " + getXsdPath(attr));
      }
//      LOG.debug("Adding nodespec {} {}", entitySpec.getClassName(), nodeSpec.getName());
    }

    for (XsdElement el : type.getChildTargetElements()) {
      String specPath = getXsdPath(type, el);
      if (elementIsValueType(el)) {
        LOG.trace("Converting element {} to node spec with path {}", el.getElementName(), specPath);
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
        if (xsdPathToNodeSpec.put(specPath, nodeSpec) != null) {
          throw new IllegalStateException("Duplicate xsd path for node spec: " + nodeSpec + ", " + getXsdPath(el));
        }
//        LOG.debug("Adding nodespec {} {}", entitySpec.getClassName(), nodeSpec.getName());
      }
      else {
        LOG.trace("Element {} with path {} is not a nodespec value", el.getElementName(), specPath);
      }
    }

    if (entitySpec.getNodeSpec("pk") != null) {
      throw new IllegalStateException("node pk already exists for entity " + entitySpec.getClassName());
    }
      NodeSpec pk = new NodeSpec();
      pk.setEntity(entitySpec);
      pk.setName("pk");
      pk.setColumnName("PK");
      pk.setJavaType(JavaType.LONG);
      pk.setJdbcType(JdbcType.BIGINT);
      pk.setNullable(Nullable.NOT_NULL);
      pk.setPrimaryKey(true);
      pk.setKeyGenSpec( getKeyGenSpec(entitySpec, pk));
      entitySpec.add(pk);
    /*} else {
      NodeSpec id = entitySpec.getNodeSpec("id");
      id.setPrimaryKey(true);
      if (id.getJdbcType() == JdbcType.VARCHAR) {
        id.setJdbcType(JdbcType.CHAR);
      }*/

    return entitySpec;
  }

  protected KeyGenSpec getKeyGenSpec(EntitySpec entitySpec, NodeSpec pk) {
    return KeyGenSpec.FRAMEWORK;
  }

  protected Integer getNodeScale(XsdAttribute attr) throws XsdDefinitionException {
    return getNodeScale(getNodeJavaType(attr));
  }

  protected Integer getNodePrecision(XsdAttribute attr) throws XsdDefinitionException {
    return getNodePrecision(getNodeJavaType(attr));
  }

  protected Nullable getNodeNullable(XsdAttribute attr) throws XsdDefinitionException {
    return attr.getUse().equals("optional") ? Nullable.NULL : Nullable.NOT_NULL;
  }

  protected Integer getNodeLength(XsdAttribute attr) throws XsdDefinitionException {
    return getNodeLength(getNodeJavaType(attr));
  }

  protected JdbcType getNodejdbcType(XsdAttribute attr) throws XsdDefinitionException {
    return getNodejdbcType(getNodeJavaType(attr));
  }

  protected JavaType getNodeJavaType(XsdAttribute attr) throws XsdDefinitionException {
    XsdType type = attr.getType();
    switch(type.getTypeLocalName()) {
      case "positiveInteger": return JavaType.INTEGER;
      default: {
//        LOG.trace("Defaulting attribute type '{}' to String.", attr.getType().getTypeLocalName());
        return JavaType.STRING;
      }
    }
  }

  protected Object getNodeFixedValue(XsdAttribute attr) {
    return null;
  }

  protected EnumSpec getNodeEnumSpec(XsdAttribute attr) {
    return null;
  }

  protected String getNodeColumnName(XsdAttribute attr) throws XsdDefinitionException {
    return getNodeColumnName(attr.getName());
  }

  protected String getNodeName(XsdAttribute attr) throws XsdDefinitionException {
    return attr.getName();
  }

  protected boolean elementIsValueType(XsdElement el) throws XsdDefinitionException {
//    if (el.getType() instanceof XsdCoreType || el.getType() instanceof XsdSimpleType) {
//      return true;
//    }
//
    return !typeHasMoreThanOneElementOrAttribute(el.getType());
  }

  protected boolean typeHasMoreThanOneElementOrAttribute(XsdType type) throws XsdDefinitionException {
    if (type.getAttributes().size() > 1) {
      return true;
    }
    if (type.getChildTargetElements().size() > 1) {
      return true;
    }
    if (type.getAttributes().size() == 1 && type.getChildTargetElements().size() == 1) {
      return true;
    }
    return false;
  }

  protected String getNodeName(XsdElement el) throws XsdDefinitionException {
    String name =  el.getElementName();
    if (isJavaKeyword(name)) {
      name += "Value";
    }
    return name;
  }

  protected boolean isJavaKeyword(String name) {
    return Arrays.asList("return", "volatile").contains(name.toLowerCase());
  }

  protected String getNodeColumnName(XsdElement el) throws XsdDefinitionException {
    return getNodeColumnName(el.getElementName());
  }
  protected String getNodeColumnName(String proposedName) throws XsdDefinitionException {
    if (isDbKeyword( proposedName )) {
      return proposedName +  "_val";
    }
    return proposedName;
  }

  protected boolean isDbKeyword(String elementName) {
    return Arrays.asList("offset", "order", "desc").contains(elementName.toLowerCase());
  }

  protected EnumSpec getNodeEnumSpec(XsdElement el) {
    return null;
  }

  protected Object getNodeFixedValue(XsdElement el) {
    return null;
  }

  protected JavaType getNodeJavaType(XsdElement el) throws XsdDefinitionException {
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
//      LOG.trace("Defaulting " + type.getTypeLocalName() + " to String");
      return JavaType.STRING;
    }
    }
  }

  protected JdbcType getNodejdbcType(XsdElement el) throws XsdDefinitionException {
    return getNodejdbcType(getNodeJavaType(el));
  }

  protected JdbcType getNodejdbcType(JavaType javaType) throws XsdDefinitionException {
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

  protected Integer getNodeLength(XsdElement el) throws XsdDefinitionException {
    return getNodeLength(getNodeJavaType(el));
  }

  protected Integer getNodeLength(JavaType javaType) throws XsdDefinitionException {
    if (javaType == JavaType.STRING) {
      return 100;
    } else if (javaType == JavaType.BIGDECIMAL) {
      return 9;
    }
    return null;

  }

  protected Nullable getNodeNullable(XsdElement el) throws XsdDefinitionException {
    return el.getMinOccurs() == 0 ? Nullable.NULL : Nullable.NOT_NULL;
  }

  protected Integer getNodePrecision(XsdElement el) throws XsdDefinitionException {
    JavaType javaType = getNodeJavaType(el);
    return getNodePrecision(javaType);
  }

  protected Integer getNodePrecision(JavaType javaType) throws XsdDefinitionException {
    if (javaType == JavaType.BIGDECIMAL) {
      return 9;
    }
    return null;
  }

  protected Integer getNodeScale(XsdElement el) throws XsdDefinitionException {
    return getNodeScale(getNodeJavaType(el));
  }

  protected Integer getNodeScale(JavaType javaType) {
    if (javaType == JavaType.BIGDECIMAL) {
      return 4;
    }
    return null;
  }

  protected String getEntityQueryClassName(XsdType type) throws XsdDefinitionException {
    return namespace + ".query.Q" + getEntityName(type);
  }

  protected String getEntityName(XsdType type) throws XsdDefinitionException {
    if (type.getTypeLocalName() != null && type.getTypeLocalName().length() > 0) {
      return type.getTypeLocalName();
    }
    XsdNode parent = type.getParent();
    if (parent instanceof XsdElement) {
      return ((XsdElement) parent).getElementName();
    }
    throw new IllegalStateException("Cannot get type name for type " + type);
  }

  protected String getEntityDtoClassName(XsdType type) throws XsdDefinitionException {
    return namespace + ".dto." + getEntityName(type) + "Dto";
  }

  protected String getEntityClassName(XsdType type) throws XsdDefinitionException {
    return namespace + ".model." + getEntityName(type);
  }

  protected String getEntityTableName(XsdType type) throws XsdDefinitionException {
    return prefix + "_" + getEntityName(type);
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

      List<NodeSpec> key = new LinkedList<>();
      for (NodeSpec ns: entitySpec.getNodeSpecs()) {
        if (ns.isPrimaryKey()) {
            key.add(ns);
        }
    }
    if (!key.isEmpty()) {
        createPrimaryKeyConstraint(entitySpec, key);
    }


      for (XsdElement el : type.getChildTargetElements()) {
        String specPath = getXsdPath(type, el);
        if (el.getMaxOccurs() != 1) {
          LOG.trace("Element {} with path {} occurs many times, skipping as FK relation...", el.getElementName(), specPath);
          continue;
        }
        if (el.getType() instanceof XsdComplexType) {
          if (!shouldBeAnEntity(el.getType())) {
            LOG.trace("Element {} with path {} has type which should NOT be an entity, skipping as FK relation", el.getElementName(), specPath);
            continue;
          }

          EntitySpec fkEntity = entitySpecsByTypeName.get(el.getType().getTypeLocalName());
          Objects.requireNonNull(fkEntity, "Cannot find entity for type {}" + el.getType());
          // normal FK relation
          LOG.trace("Converting element {} with path {} to FK relation to {}", el.getElementName(), specPath, fkEntity.getClassName());
          NodeSpec fkNodeSpec = new NodeSpec();
          fkNodeSpec.setEntity(entitySpec);
          fkNodeSpec.setName(getNodeName(el));
          fkNodeSpec.setColumnName(getFkColumnName(el));
          fkNodeSpec.setNullable(getNodeNullable(el));
          fkNodeSpec.setJdbcType( getPrimaryKeyType(fkEntity) );;
          RelationSpec rspec = new RelationSpec();
          rspec.setEntitySpec(fkEntity);
          rspec.setJoinType(JoinTypeSpec.LEFT_OUTER_JOIN);
          rspec.setType(RelationType.REFERS);
          fkNodeSpec.setRelation(rspec);

          if (xsdPathToNodeSpec.put(specPath, fkNodeSpec) != null) {
            throw new IllegalStateException("Duplicate xsd path for node spec: " + fkNodeSpec.getName() + ", " + specPath);
          }

          entitySpec.add(fkNodeSpec);


          createForeignKeyConstraint(entitySpec, fkNodeSpec, rspec);
          LOG.debug("Added FK relation from {}.{} to {}", entitySpec.getClassName(), fkNodeSpec.getName(),
              fkEntity.getClassName());
        }
        else {
          LOG.trace("Element {} with path {} is not a complex type, skipping as FK relation", el.getElementName(), specPath);
        }
      }
    }
  }

  private JdbcType getPrimaryKeyType(EntitySpec fkEntity) {
    if (fkEntity.getPrimaryKeyNodes(true).size() > 1) {
      throw new IllegalStateException("Composite PK not expected");
    }
    return fkEntity.getPrimaryKeyNodes(true).iterator().next().getJdbcType();
  }

  protected void postProcess() {
    logWarningsForSmallEntities();
//    for (Map.Entry<String, EntitySpec> entry: xsdPathToEntitySpec.entrySet()) {
//      System.out.println(entry.getKey() + "  :  " + entry.getValue().getClassName());
//    }
  }

  private void logWarningsForSmallEntities() {
    for (EntitySpec entitySpec: entitySpecs.values()) {
      if (entitySpec.getNodeSpecs().size() < 2) {
          LOG.warn("Entity {} has only {} nodes", entitySpec.getClassName(), entitySpec.getNodeSpecs().size());
      }
      else if (entitySpec.getNodeSpecs().size() == 2) {
        LOG.warn("Entity {} has only 2 nodes: {}", entitySpec.getClassName(), printNodesShort(entitySpec));

      }
    }
  }

  private String printNodesShort(EntitySpec entitySpec) {
    StringBuilder sb = new StringBuilder();
    for (NodeSpec ns: entitySpec.getNodeSpecs()) {
      sb.append(ns.getName());
      sb.append(", ");
    }
    sb.setLength(sb.length() - 2);
    return sb.toString();
  }

  protected String getFkColumnName(XsdElement el) throws XsdDefinitionException {
    return getNodeName(el) + "_id";
  }

  private final Set<String> existingFkConstraintNames = new HashSet<>();

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
    existingFkConstraintNames.add(keySpecName);
  }


  protected String createForeignKeyConstraintName(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
    String name =  "fk_" + shortTableName(entitySpec.getTableName()) + "_" + shortTableName(relationSpec.getEntitySpec().getTableName());
    while (existingFkConstraintNames.contains(name)) {
      name = "_" + name;
    }
    return name;
  }

  private String shortTableName(String text) {
    if (text.substring(prefix.length() + 1).length() <= 10) {
      return text;
    }
    return text.substring(prefix.length() + 1, 10 + prefix.length() + 1);
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
