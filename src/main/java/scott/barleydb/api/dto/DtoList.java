package scott.barleydb.api.dto;

import java.util.LinkedList;

public class DtoList<T> extends LinkedList<T> {

  private static final long serialVersionUID = 1L;

  /**
   * if the list has been fetched or not (true, false or undefined)
   */
  private Boolean fetched;

  public DtoList() {
    super();
  }


   public Boolean isFetched() {
    return fetched;
  }

  public void setFetched(Boolean value) {
    this.fetched = value;
  }
}
