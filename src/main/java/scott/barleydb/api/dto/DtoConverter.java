package scott.barleydb.api.dto;

import java.util.Arrays;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextState;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.BarleyDBRuntimeException;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;

@SuppressWarnings("unchecked")
public class DtoConverter {

  private static final Logger LOG = LoggerFactory.getLogger(DtoConverter.class);

  private final String namespace;
  private final Environment env;
  private final DtoHelper helper;
  private final Map<UUID, BaseDto> dtos;
  private final Set<UUID> imported;
  private final Set<Entity> entites = new HashSet<>();
  private EntityContext ctx;

  public DtoConverter(EntityContext ctx) {
    this.env = ctx.getEnv();
    this.namespace = ctx.getNamespace();
    this.ctx = ctx;
    this.helper = new DtoHelper(env, namespace);
    this.dtos = new HashMap<>();
    this.imported = new HashSet<>();
  }

  public DtoConverter(Environment env, String namespace, EntityContext ctx) {
    this.env = env;
    this.namespace = namespace;
    this.helper = new DtoHelper(env, namespace);
    this.dtos = new HashMap<>();
    this.imported = new HashSet<>();
    this.ctx = ctx;
  }

  public EntityContext getEntityContext() {
    return ctx;
  }

  public <T extends ProxyController> T getModel(BaseDto dto) {
    Entity entity = getEntity(dto);
    return (T)(entity != null ? ctx.getProxy(entity) : null);
  }

  public Entity getEntity(BaseDto dto) {
    if (ctx == null) {
      return null;
    }
    return ctx.getEntityByUuid(dto.getBaseDtoUuid(), false);
  }

  public <T extends BaseDto> T getDto(ProxyController proxy) {
    BaseDto dto = dtos.get( proxy.getEntity().getUuid());
    return (T)dto;
  }


  /**
   * Converts DTO graphs into a set of managed Entities
   * @param dto
   * @param type
   * @return
   * @throws BarleyDBQueryException
   * @throws SortServiceProviderException
   */
  public void importDtos(BaseDto ...dtos) throws SortServiceProviderException, BarleyDBQueryException {
    importDtos(Arrays.asList(dtos));
  }
  public void importDtos(List<? extends BaseDto> dtos) throws SortServiceProviderException, BarleyDBQueryException {
    LOG.debug("Converting DTOS into entities...");
    if (ctx == null) {
      ctx = new EntityContext(env, namespace);
    }
    for (BaseDto dto: dtos) {
      collectedDtosFromObjectGraph(dto);
    }
    importDtosAsEntities();
    logMappingReport();
  }

  /**
   * Converts Entity graphs into DTO graphs
   * @param proxy
   * @param type
   * @return
   */
  public void convertToDtos(ProxyController ...proxies) {
    LOG.debug("Converting entities into DTOs...");
    for (ProxyController proxy: proxies) {
      Entity entity = proxy.getEntity();
      if (ctx == null) {
        ctx = entity.getEntityContext();
      }
      else if (ctx != entity.getEntityContext()) {
        throw new BarleyDBRuntimeException("Entity '" + entity + "' does not share the same context");
      }
      Collection<Entity> entities = collectedEntitiesFromEntityGraph(entity);

      exportDtos(entities);
    }
  }

  /**
   * Converts Entity graphs into DTO graphs
   * @param proxy
   * @param type
   * @return
   */
  public void convertToDtos() {
    if (ctx == null) {
      return;
    }
    LOG.debug("Converting entities into DTOs...");
    EntityContextState mode = ctx.switchToInternalMode();
    try {
      for (Entity entity: ctx.getEntities()) {
        Collection<Entity> entities = collectedEntitiesFromEntityGraph(entity);

        exportDtos(entities);
      }
      logMappingReport();
    }
    finally {
      ctx.switchToMode(mode);
    }
  }

  private void collectedDtosFromObjectGraph(BaseDto dto) {
    BaseDto existingDto = dtos.get(dto.getBaseDtoUuid());
    if (existingDto == dto) {
      return;
    }
    if (existingDto != null) {
      throw new IllegalStateException("Duplicate UUID in DTO graph");
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace("Collecting DTOs referenced by {} {}", dto, dto.getBaseDtoUuidFirst7());
    }
    dtos.put(dto.getBaseDtoUuid(), dto);
    for (Object propValue: helper.getProperties(dto).values()) {
      if (propValue instanceof BaseDto) {
        collectedDtosFromObjectGraph((BaseDto)propValue);
      }
      else if (propValue instanceof DtoList) {
        DtoList<BaseDto> list = (DtoList<BaseDto>)propValue;
        for (BaseDto item: list) {
          collectedDtosFromObjectGraph(item);
        }
      }
    }
  }

  private void importDtosAsEntities() throws SortServiceProviderException, BarleyDBQueryException {
    for (BaseDto dto: dtos.values()) {
      EntityType et = env.getDefinitions(namespace).getEntityTypeForDtoClass(dto.getClass(), true);
      Map<String,Object> properties = helper.getProperties(dto);
      Object key = properties.get(et.getKeyColumn());
      Entity e = ctx.getEntityByUuid(dto.getBaseDtoUuid(), false);
      if (e == null) {
        e = ctx.newEntity(et, key, dto.getConstraints(), dto.getBaseDtoUuid());
        entites.add(e);
        if (LOG.isDebugEnabled()) {
          LOG.trace("{}:  Created entity {} for DTO {}", dto.getBaseDtoUuidFirst7(), e, dto);
        }
      }
      else {
        entites.add(e);
        e.getConstraints().set(dto.getConstraints());
        if (LOG.isDebugEnabled()) {
          LOG.trace("{}:  Entity {} already exists for DTO {}", dto.getBaseDtoUuidFirst7(), e, dto);
        }
      }
    }

    IdentityHashMap<BaseDto, Map<String,Object>> cache = new IdentityHashMap<>();
    for (BaseDto dto: dtos.values()) {
      Entity entity = ctx.getEntityByUuid(dto.getBaseDtoUuid(), true);
      entity.setEntityState(dto.getEntityState());
      if (LOG.isTraceEnabled()) {
        LOG.trace("{}:  Copying {} entity state from DTO", entity.getUuidFirst7(), entity.getEntityState());
      }
      Map<String,Object> propValues = helper.getProperties(dto);
      cache.put(dto, propValues);
      copyValues(propValues, entity);
    }
    for (BaseDto dto: dtos.values()) {
      Map<String,Object> propValues = cache.get(dto);
      Entity entity = ctx.getEntityByUuid(dto.getBaseDtoUuid(), true);
      copyReferences(propValues, entity);
    }
    for (BaseDto dto: dtos.values()) {
      Map<String,Object> propValues = cache.get(dto);
      Entity entity = ctx.getEntityByUuid(dto.getBaseDtoUuid(), true);
      copyCollections(propValues, entity);
      imported.add(dto.getBaseDtoUuid());
    }
  }

  private void logMappingReport() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Mapped " + dtos.values().size() + " dtos and entities");
//      for(Map.Entry<UUID, BaseDto> entry: dtos.entrySet()) {
//        LOG.debug("-----------------------------------------------------");
//        BaseDto dto = entry.getValue();
//        LOG.debug(String.format("DTO    %-12s %40s %-15s", dto.getBaseDtoUuidFirst7(), dto, dto.getEntityState()));
//        Entity e = ctx.getEntityByUuid(dto.getBaseDtoUuid(), true);
//        LOG.debug(String.format("Entity %-12s %-15s", e.getUuidFirst7(), e));
//      }
    }
  }

  private Collection<Entity> collectedEntitiesFromEntityGraph(Entity entity) {
      Map<UUID, Entity> result = new HashMap<>();
      collectedEntitiesFromEntityGraph(result, entity);
      return result.values();
  }

  private void collectedEntitiesFromEntityGraph(Map<UUID, Entity> result, Entity entity) {
    if (result.put(entity.getUuid(), entity) != null) {
      return;
    }
    for (RefNode ref: entity.getChildren(RefNode.class)) {
      if (ref.getReference() != null) {
          collectedEntitiesFromEntityGraph(result, ref.getReference());
      }
    }
    for (ToManyNode toMany: entity.getChildren(ToManyNode.class)) {
      for (Entity e: toMany.getList()) {
          collectedEntitiesFromEntityGraph(result, e);
      }
    }
  }

  private void exportDtos(Collection<Entity> entities) {
    for (Entity entity: entities) {
      if (!dtos.containsKey(entity.getUuid())) {
        dtos.put(entity.getUuid(), createNewDto(entity));
      }
    }
    for (Entity e: entities) {
       BaseDto dto = dtos.get(e.getUuid());
       dto.setEntityState(e.getEntityState());
       copyValuesNodes(e, dto);
    }
    for (Entity e: entities) {
      BaseDto dto = dtos.get(e.getUuid());
      copyRefNodes(e, dto);
    }
    for (Entity e: entities) {
       BaseDto dto = dtos.get(e.getUuid());
       copyToManyNodes(e, dto);
    }
  }

  private void copyToManyNodes(Entity entity, BaseDto dto) {
    for (ToManyNode node: entity.getChildren(ToManyNode.class)) {
      if (node.getNodeType().isSuppressedFromDto()) {
        continue;
      }
      helper.clearCollection(dto, node.getNodeType(), dto);
      for (Entity reffedEntity:  node.getList()) {
        if (node.getNodeType().getJoinProperty() == null) {
          BaseDto reffedDto = dtos.get(reffedEntity.getUuid());
          helper.addToCollection(dto, node.getNodeType(), reffedDto);
        }
        else {
          RefNode onwardRef = reffedEntity.getChild(node.getNodeType().getJoinProperty(), RefNode.class);
          BaseDto reffedDto = dtos.get(onwardRef.getReference().getUuid());
          helper.addToCollection(dto, node.getNodeType(), reffedDto);
        }
      }
      helper.setCollectionFetched(dto, node.getNodeType(), node.isFetched());
    }
  }

  private void copyRefNodes(Entity entity, BaseDto dto) {
    for (RefNode node: entity.getChildren(RefNode.class)) {
      if (node.getNodeType().isSuppressedFromDto()) {
        continue;
      }
      Entity reffedEntity = node.getReference();
      if (reffedEntity != null) {
        BaseDto reffedDto = dtos.get(reffedEntity.getUuid());
        helper.setProperty(dto, node.getNodeType(), reffedDto);
      }
    }
  }

  private void copyValuesNodes(Entity entity, BaseDto dto) {
    for (ValueNode node: entity.getChildren(ValueNode.class)) {
      if (node.getNodeType().isSuppressedFromDto()) {
        continue;
      }
      helper.setProperty(dto, node.getNodeType(), node.getValueNoFetch());
    }
  }

  private BaseDto createNewDto(Entity entity) {
    String dtoClassName = entity.getEntityType().getDtoClassName();
    BaseDto dto;
    try {
      dto = (BaseDto)(getClass().getClassLoader().loadClass(dtoClassName)).newInstance();
      dto.setBaseDtoUuid( entity.getUuid() );
      return dto;
    }
    catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
       throw new BarleyDBRuntimeException("Could not instantiate DTO", e);
    }
  }

  /**
   * sets up the entity ToManyNode contents to mirror the corresponding Dto list
   * @param propValues
   * @param entity
   * @throws BarleyDBQueryException
   * @throws SortServiceProviderException
   */
  private void copyCollections(Map<String,Object> propValues, Entity entity) throws SortServiceProviderException, BarleyDBQueryException {
    try {
      for (ToManyNode node: entity.getChildren(ToManyNode.class)) {
        if (node.getNodeType().isSuppressedFromDto()) {
          continue;
        }
        if (LOG.isTraceEnabled()) {
          LOG.trace("{}:  Mapping collection for entity {} and property '{}'", entity.getUuidFirst7(), entity, node.getName());
        }
        DtoList<? extends BaseDto> list = (DtoList<? extends BaseDto>) propValues.get(node.getName());
        if (list != null) {
          if (list.isFetched() == null) {
            throw new BarleyDBRuntimeException("DtoList fetch status is not set for " + entity.getName() + " and " + node.getName());
          }
          node.setFetched(list.isFetched());
          if (node.getNodeType().getJoinProperty() == null) {
            //we are dealing with a normal 1:N relation - get the entities which match the dto list entries
            //and add them to the tomany node
            for (Entity e: getEntitiesForDtoList(list))  {
              node.addIfAbsent(e);
              if (LOG.isTraceEnabled()) {
                LOG.trace("{}:  Added {} into list '{}'", entity.getUuidFirst7(), e, node.getName());
              }
            }
          }
          else {
            if (LOG.isTraceEnabled()) {
              LOG.trace("{}:  Entity property {}.'{}' is an N:M relation with joinProperty '{}'", entity.getUuidFirst7(), entity.getEntityType().getInterfaceShortName(), node.getName(), node.getNodeType().getJoinProperty());
            }
            /* we have an join property so this is a N:M  relation, the DTO just directly listed the M side and so we have to load the N from the database now
             so that the entity model is correct. */
            //so get the M entities
            List<Entity> entities = getEntitiesForDtoList(list);
            //use those M entities to setup the N:M relation (returned as the map)
            Map<Entity,Entity> nToM = setupNToMRelation(node, entities);
            //add the nside of the map (the keyset) to the tomany node
            for (Entity nSide: nToM.keySet()) {
              node.add(nSide);
            }
          }
        }
      }
    }
    catch(ClassCastException x) {
      throw new BarleyDBRuntimeException("Dto list does not extend DtoList", x);
    }
  }

  private Map<Entity, Entity> setupNToMRelation(ToManyNode node, List<Entity> entities) throws SortServiceProviderException, BarleyDBQueryException {
    //get the query for the entity type with the ToManyNode (the template which refers to the templatebusinesstype)
    Entity entity = node.getParent();
    QueryResult<Object> queryResult = null;
    Map<Entity,Entity> result = new LinkedHashMap<>();
    EntityType joinEntityType = ctx.getDefinitions().getEntityTypeMatchingInterface(node.getNodeType().getRelationInterfaceName(), true);
    String joinProperty = node.getNodeType().getJoinProperty();

    Set<Entity> toProcess = new HashSet<>(entities);
    if (entity.getKeyValue() != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("{}:  Executing query (in separate ctx) to load and reuse existing join table records for entity {}", entity.getUuidFirst7(), entity);
      }
      /*
       * The entity (the N side of the N:M relation has a PK), so we load the missing Join entity data.
       * Which may not exist if the entity is not yet in the database
       */
      EntityContext ctx = this.ctx.newEntityContextSharingTransaction();
      QueryObject<Object> q = ctx.getUnitQuery( entity.getEntityType() );
      QPropertyCondition qcond = new QProperty<>(q, entity.getKey().getName() ).equal(entity.getKeyValue());
      q.where(qcond);

      //left outer join to the join entity (template -> template busines type).
      QueryObject<Object> qToJoin = ctx.getUnitQuery( joinEntityType );
      q.addLeftOuterJoin(qToJoin, node.getName());

      queryResult = ctx.performQuery(q);
      /*
       * get the existing join entities and integrate them into our entity graph
       */
      for (Entity resultEntity: queryResult.getEntityList()) {
        ToManyNode resultNode = resultEntity.getChild(node.getName(), ToManyNode.class);
        for (Entity joinEntity: resultNode.getList()) {
          if (joinEntityReferencesOneOf(joinEntity, joinProperty, entities)) {
            if (LOG.isTraceEnabled()) {
              LOG.trace("{}:  Found join entity to import into context {}", entity.getUuidFirst7(), joinEntity);
            }
            Entity addedJoinEntity = this.ctx.copyInto(joinEntity);
            //mside must also be in 'entities'
            Entity mside = addedJoinEntity.getChild(joinProperty, RefNode.class).getReference();
            if (!toProcess.remove(mside)) {
              throw new IllegalStateException("Did not find entity as expected");
            }
            result.put(addedJoinEntity, mside);
          }
        }
       }
    }

      /*
       * We need to construct the join entities for unprocessed M entities
       *
       */
      for (Entity e: toProcess) {
          Entity joinE = this.ctx.newEntity(joinEntityType, null, EntityConstraint.mustNotExistInDatabase());
          entities.add(joinE);
          if (LOG.isTraceEnabled()) {
            LOG.trace("{}: Created new entity {} for missing join record between {} and {}", entity.getUuidFirst7(), joinE, entity, e);
          }
          //set the M reference in the join entity
          joinE.getChild(joinProperty, RefNode.class).setReference(e);
          //set the N reference in the join entity
          //get the name of the node which in the join entity which point back to us (the template reference in the template business type entity).
          String foreignNodeName = node.getNodeType().getForeignNodeName();
          joinE.getChild(foreignNodeName, RefNode.class).setReference(node.getParent());
          //add to result
          result.put(joinE, e);
      }
    return result;
  }

  private boolean joinEntityReferencesOneOf(Entity joinEntity, String joinProperty, List<Entity> entities) {
    Entity toMatch = joinEntity.getChild(joinProperty, RefNode.class).getReference();
    for (Entity e: entities) {
      if (Objects.equals(toMatch.getKeyValue(), e.getKeyValue())) {
        return true;
      }
    }
    return false;
  }

  private List<Entity> getEntitiesForDtoList(DtoList<? extends BaseDto> list) {
    List<Entity> result = new LinkedList<>();
    for (BaseDto dto: list) {
      result.add(ctx.getEntityByUuid(dto.getBaseDtoUuid(), true));
    }
    return result;
  }

  private void copyReferences(Map<String,Object> propValues, Entity entity) {
    try {
      for (RefNode node: entity.getChildren(RefNode.class)) {
        if (node.getNodeType().isSuppressedFromDto()) {
          continue;
        }
        BaseDto reffedDto = (BaseDto) propValues.get(node.getName());
        if (reffedDto != null) {
          Entity reffedEntity = ctx.getEntityByUuid(reffedDto.getBaseDtoUuid(), true);
          node.setReference( reffedEntity );
          if (LOG.isTraceEnabled()) {
            LOG.trace("{}:  {} Set entity reference --> {}", entity.getUuidFirst7(), entity, reffedEntity);
          }
        }
      }
    }
    catch(ClassCastException x) {
      throw new BarleyDBRuntimeException("Dto reference does not extend BaseDto", x);
    }
  }

  private void copyValues(Map<String,Object> propValues, Entity entity) {
    for (ValueNode node: entity.getChildren(ValueNode.class)) {
      if (node.getNodeType().isSuppressedFromDto()) {
        continue;
      }
      if (node.getNodeType().getFixedValue() == null) {
          node.setValue(propValues.get(node.getName()));
          if (LOG.isTraceEnabled()) {
            LOG.trace("{}:  Copied value {} to entity {}", node.getParent().getUuidFirst7(), node.getValueNoFetch(), node.getParent());
          }
      }
    }
  }

  public <T extends ProxyController> List<T> getModels(List<? extends BaseDto> dtoModels) {
    List<T> result = new LinkedList<>();
    for (BaseDto model: dtoModels) {
      T m = (T)getModel(model);
      Objects.requireNonNull(m, "Could not find entity model for DTO " + model.getBaseDtoUuidFirst7());
      result.add( m );
    }
    return result;
  }

  public <T extends BaseDto> List<T> getDtos(List<? extends ProxyController> models, Class<T> type) {
    List<T> result = new LinkedList<>();
    for (ProxyController model: models) {
      result.add( (T)getDto(model) );
    }
    return result;
  }

}
