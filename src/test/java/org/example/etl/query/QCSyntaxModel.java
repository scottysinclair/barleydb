package org.example.etl.query;

import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CSyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCSyntaxModel extends QAbstractCSyntaxModel<CSyntaxModel, QCSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QCSyntaxModel() {
    super(CSyntaxModel.class);
  }

  public QCSyntaxModel(QueryObject<?> parent) {
    super(CSyntaxModel.class, parent);
  }

}