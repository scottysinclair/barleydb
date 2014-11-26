package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.RawData;

/**
 * Generated from Entity Specification on Tue Nov 25 08:01:03 CET 2014
 *
 * @author scott
 */
public class QRawData extends QueryObject<RawData> {
  private static final long serialVersionUID = 1L;
  public QRawData() {
    super(RawData.class);
  }

  public QRawData(QueryObject<?> parent) {
    super(RawData.class, parent);
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