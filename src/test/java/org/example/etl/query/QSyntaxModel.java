package org.example.etl.query;

import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.SyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QSyntaxModel extends QAbstractSyntaxModel<SyntaxModel, QSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QSyntaxModel() {
    super(SyntaxModel.class);
  }

  public QSyntaxModel(QueryObject<?> parent) {
    super(SyntaxModel.class, parent);
  }

}