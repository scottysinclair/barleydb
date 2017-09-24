package scott.barleydb.api.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.SortRuntimeException;

@SuppressWarnings("unchecked")
public class DtoConverter {

  private final String namespace;
  private final Environment env;
  private final DtoHelper helper;
  private Map<UUID, BaseDto> dtos;
  private EntityContext ctx;

  public DtoConverter(Environment env, String namespace) {
    this.env = env;
    this.namespace = namespace;
    this.helper = new DtoHelper(env, namespace);
    this.dtos = new HashMap<>();
    this.ctx = null;
  }

  public <T extends ProxyController> T getModel(BaseDto dto) {
    if (ctx == null) {
      return null;
    }
    Entity entity = ctx.getEntityByUuid(dto.getUuid(), false);
    return (T)(entity != null ? ctx.getProxy(entity) : null);
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
   */
  public void importDtos(BaseDto ...dtos) {
    if (ctx == null) {
      ctx = new EntityContext(env, namespace);
    }
    for (BaseDto dto: dtos) {
      collectedDtosFromObjectGraph(dto);

      importDtosAsEntities();
    }
  }

  /**
   * Converts Entity graphs into DTO graphs
   * @param proxy
   * @param type
   * @return
   */
  public void convertToDtos(ProxyController ...proxies) {
    for (ProxyController proxy: proxies) {
      Entity entity = proxy.getEntity();
      if (ctx == null) {
        ctx = entity.getEntityContext();
      }
      else if (ctx != entity.getEntityContext()) {
        throw new SortRuntimeException("Entity '" + entity + "' does not share the same context");
      }
      Collection<Entity> entities = collectedEntitiesFromEntityGraph(entity);

      exportDtos(entities);
    }
  }

  private void collectedDtosFromObjectGraph(BaseDto dto) {
    BaseDto existingDto = dtos.get(dto.getUuid());
    if (existingDto == dto) {
      return;
    }
    if (existingDto != null) {
      throw new IllegalStateException("Duplicate UUID in DTO graph");
    }
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

  private void importDtosAsEntities() {
    for (BaseDto dto: dtos.values()) {
      EntityType et = env.getDefinitions(namespace).getEntityTypeForDtoClass(dto.getClass(), true);
      Map<String,Object> properties = helper.getProperties(dto);
      Object key = properties.get(et.getKeyColumn());
      ctx.newEntity(et, key, dto.getConstraints(), dto.getUuid());
    }

    for (BaseDto dto: dtos.values()) {
        Map<String,Object> propValues = helper.getProperties(dto);
        Entity entity = ctx.getEntityByUuid(dto.getUuid(), true);
        copyValues(propValues, entity);
        copyReferences(propValues, entity);
        copyCollections(propValues, entity);
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
    for (BaseDto dto: dtos.values()) {
       copyValuesNodes(ctx.getEntityByUuid(dto.getUuid(), true), dto);
       copyRefNodes(ctx.getEntityByUuid(dto.getUuid(), true), dto);
       copyToManyNodes(ctx.getEntityByUuid(dto.getUuid(), true), dto);
    }
  }

  private void copyToManyNodes(Entity entity, BaseDto dto) {
    for (ToManyNode node: entity.getChildren(ToManyNode.class)) {
      for (Entity reffedEntity:  node.getList()) {
        BaseDto reffedDto = dtos.get(reffedEntity.getUuid());
        helper.addToCollection(dto, node.getNodeType(), reffedDto);
      }
      helper.setCollectionFetched(dto, node.getNodeType(), node.isFetched());
    }
  }

  private void copyRefNodes(Entity entity, BaseDto dto) {
    for (RefNode node: entity.getChildren(RefNode.class)) {
      Entity reffedEntity = node.getReference();
      if (reffedEntity != null) {
        BaseDto reffedDto = dtos.get(reffedEntity.getUuid());
        helper.setProperty(dto, node.getNodeType(), reffedDto);
      }
    }
  }

  private void copyValuesNodes(Entity entity, BaseDto dto) {
    for (ValueNode node: entity.getChildren(ValueNode.class)) {
      helper.setProperty(dto, node.getNodeType(), node.getValueNoFetch());
    }
  }

  private BaseDto createNewDto(Entity entity) {
    String dtoClassName = entity.getEntityType().getDtoClassName();
    BaseDto dto;
    try {
      dto = (BaseDto)(getClass().getClassLoader().loadClass(dtoClassName)).newInstance();
      dto.setUuid( entity.getUuid() );
      return dto;
    }
    catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
       throw new SortRuntimeException("Could not instantiate DTO", e);
    }
  }

  private void copyCollections(Map<String,Object> propValues, Entity entity) {
    try {
      for (ToManyNode node: entity.getChildren(ToManyNode.class)) {
        DtoList<? extends BaseDto> list = (DtoList<? extends BaseDto>) propValues.get(node.getName());
        if (list != null) {
          node.setFetched(list.isFetched());
          for (BaseDto dto: list) {
            Entity e = ctx.getEntityByUuid(dto.getUuid(), true);
            node.add(e);
          }
        }
      }
    }
    catch(ClassCastException x) {
      throw new SortRuntimeException("Dto list does not extend DtoList", x);
    }
  }

  private void copyReferences(Map<String,Object> propValues, Entity entity) {
    try {
      for (RefNode node: entity.getChildren(RefNode.class)) {
        BaseDto reffedDto = (BaseDto) propValues.get(node.getName());
        if (reffedDto != null) {
          Entity reffedEntity = ctx.getEntityByUuid(reffedDto.getUuid(), true);
          node.setReference( reffedEntity );
        }
      }
    }
    catch(ClassCastException x) {
      throw new SortRuntimeException("Dto reference does not extend BaseDto", x);
    }
  }

  private void copyValues(Map<String,Object> propValues, Entity entity) {
    for (ValueNode node: entity.getChildren(ValueNode.class)) {
      node.setValueNoEvent(propValues.get(node.getName()));
    }
  }

}
