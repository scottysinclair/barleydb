package com.smartstream.messaging.model;

import java.util.List;

public interface CsvStructure extends Structure {

    List<CsvStructureField> getFields();

}
