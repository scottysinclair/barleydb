package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CRawData;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCRawData extends QueryObject<CRawData> {
  private static final long serialVersionUID = 1L;
  public QCRawData() {
    super(CRawData.class);
  }

  public QCRawData(QueryObject<?> parent) {
    super(CRawData.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<byte[]> data() {
    return new QProperty<byte[]>(this, "data");
  }

  public QProperty<String> characterEncoding() {
    return new QProperty<String>(this, "characterEncoding");
  }
}