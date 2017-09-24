package scott.barleydb.api.dto;

import java.io.Serializable;
import java.util.UUID;

import scott.barleydb.api.core.entity.EntityConstraint;

public class BaseDto implements Serializable {

  private static final long serialVersionUID = 1L;
 /**
   * UUID for housekeeping.
   */
  private UUID uuid = UUID.randomUUID();
  /**
   * entity constraints
   */
  private EntityConstraint constraints;

  public EntityConstraint getConstraints() {
    return constraints;
  }

  public void setConstraints(EntityConstraint constraints) {
    this.constraints = constraints;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }


}
