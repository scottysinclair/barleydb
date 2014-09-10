package com.smartstream.messaging.query;

import com.smartstream.mac.query.QUser;
import com.smartstream.messaging.model.CsvSyntaxModel;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

public class QCsvSyntaxModel extends QueryObject<CsvSyntaxModel> {

    private static final long serialVersionUID = 1L;

    public QCsvSyntaxModel() {
        this(null);
    }

    public QCsvSyntaxModel(QueryObject<?> parent) {
        super(CsvSyntaxModel.class, parent);
    }

    public QCsvMapping joinToMappings() {
        QCsvMapping mapping = new QCsvMapping();
        addJoin(mapping, "mappings");
        return mapping;
    }

    public QUser joinToUser() {
        QUser user = new QUser();
        addJoin(user, "user");
        return user;
    }

    public QCsvStructure joinToStructure() {
        QCsvStructure structure = new QCsvStructure();
        addJoin(structure, "structure");
        return structure;
    }

    public QUser existsUser() {
        QUser user = new QUser(this);
        addExists(user, "user");
        return user;
    }

    public QCsvStructure existsStructure() {
        QCsvStructure structure = new QCsvStructure(this);
        addExists(structure, "structure");
        return structure;
    }

    public QProperty<Integer> syntaxType() {
        return new QProperty<Integer>(this, "syntaxType");
    }

    public QProperty<String> syntaxName() {
        return new QProperty<String>(this, "name");
    }

}