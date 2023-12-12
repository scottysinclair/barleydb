package scott.barleydb.api.core.entity.context;

import java.util.Objects;

import scott.barleydb.api.config.EntityType;

public class EntityId {

   private final EntityType entityType;

   private final Object key;

   public EntityId(final EntityType entityType, final Object key) {
      this.entityType = entityType;
      this.key = key;
   }

   public EntityType getEntityType() {
      return entityType;
   }

   public Object getKey() {
      return key;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {return true;}
      if (o == null || getClass() != o.getClass()) {return false;}
      final EntityId entityId = (EntityId) o;
      return Objects.equals(entityType, entityId.entityType) && Objects.equals(key, entityId.key);
   }

   @Override
   public int hashCode() {
      return Objects.hash(entityType, key);
   }
}
