---
--- Schema generated by BarleyDB static definitions ---
---
---

create table ACL_USER (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  USER_NAME VARCHAR(50) NOT NULL
);

create table ACL_ACCESS_AREA (
  ID BIGINT NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  PARENT_ID BIGINT NULL
);

alter table ACL_USER add constraint PK_USER primary key (ID);
alter table ACL_ACCESS_AREA add constraint PK_ACCESS_AREA primary key (ID);

alter table ACL_USER add constraint FK_USER_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table ACL_ACCESS_AREA add constraint FK_ACCESS_AREA_ACCESS_AREA foreign key (PARENT_ID) references ACL_ACCESS_AREA(ID);

alter table ACL_USER add constraint UC_USER_1 unique (USER_NAME,ACCESS_AREA_ID);

create table SS_SYNTAX_MODEL (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  STRUCTURE_TYPE INTEGER NOT NULL,
  SYNTAX_TYPE INTEGER NOT NULL,
  USER_ID BIGINT NULL,
  STRUCTURE_ID BIGINT NOT NULL
);

create table SS_XMLSTRUCTURE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL
);

create table SS_XML_MAPPING (
  ID BIGINT NOT NULL,
  SYNTAX_MODEL_ID BIGINT NOT NULL,
  SUB_SYNTAX_ID BIGINT NULL,
  XPATH VARCHAR(150) NULL,
  TARGET_FIELD_NAME VARCHAR(150) NULL
);

create table SS_CSVSTRUCTURE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  HEADER_BASED_MAPPING INTEGER NOT NULL
);

create table SS_CSVSTRUCTURE_FIELD (
  ID BIGINT NOT NULL,
  NAME VARCHAR(50) NULL,
  CSVSTRUCTURE_ID BIGINT NOT NULL,
  COLUMN_INDEX INTEGER NOT NULL,
  OPTIONAL INTEGER NOT NULL
);

create table SS_CSV_MAPPING (
  ID BIGINT NOT NULL,
  SYNTAX_MODEL_ID BIGINT NOT NULL,
  CSVSTRUCTURE_FIELD_ID BIGINT NOT NULL,
  TARGET_FIELD_NAME VARCHAR(150) NULL
);

create table SS_TEMPLATE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL
);

create table SS_TEMPLATE_CONTENT (
  ID BIGINT NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  TEMPLATE_ID BIGINT NOT NULL
);

create table SS_DATATYPE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL
);

create table SS_TEMPLATE_DATATYPE (
  ID BIGINT NOT NULL,
  TEMPLATE_ID BIGINT NOT NULL,
  DATATYPE_ID BIGINT NOT NULL
);

create table SS_RAWDATA (
  ID BIGINT NOT NULL,
  DATA VARBINARY(1073741824) NOT NULL,
  CHARACTER_ENCODING VARCHAR(50) NULL
);

create table XSS_XSYNTAX_MODEL (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  STRUCTURE_TYPE INTEGER NOT NULL,
  SYNTAX_TYPE INTEGER NOT NULL,
  USER_ID BIGINT NULL,
  STRUCTURE_ID BIGINT NOT NULL
);

create table XSS_XXMLSTRUCTURE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL
);

create table XSS_XXML_MAPPING (
  ID BIGINT NOT NULL,
  XSYNTAX_MODEL_ID BIGINT NOT NULL,
  SUB_SYNTAX_ID BIGINT NULL,
  XPATH VARCHAR(150) NULL,
  TARGET_FIELD_NAME VARCHAR(150) NULL
);

create table XSS_XCSVSTRUCTURE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  HEADER_BASED_MAPPING INTEGER NOT NULL
);

create table XSS_XCSVSTRUCTURE_FIELD (
  ID BIGINT NOT NULL,
  NAME VARCHAR(50) NULL,
  XCSVSTRUCTURE_ID BIGINT NOT NULL,
  COLUMN_INDEX INTEGER NOT NULL,
  OPTIONAL INTEGER NOT NULL
);

create table XSS_XCSV_MAPPING (
  ID BIGINT NOT NULL,
  XSYNTAX_MODEL_ID BIGINT NOT NULL,
  XCSVSTRUCTURE_FIELD_ID BIGINT NOT NULL,
  TARGET_FIELD_NAME VARCHAR(150) NULL
);

create table XSS_XTEMPLATE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL
);

create table XSS_XTEMPLATE_CONTENT (
  ID BIGINT NOT NULL,
  NAME VARCHAR(50) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  XTEMPLATE_ID BIGINT NOT NULL
);

create table XSS_XDATATYPE (
  ID BIGINT NOT NULL,
  ACCESS_AREA_ID BIGINT NOT NULL,
  UUID CHAR(60) NOT NULL,
  MODIFIED_AT TIMESTAMP NOT NULL,
  NAME VARCHAR(50) NOT NULL
);

create table XSS_XTEMPLATE_DATATYPE (
  ID BIGINT NOT NULL,
  XTEMPLATE_ID BIGINT NOT NULL,
  XDATATYPE_ID BIGINT NOT NULL
);

create table XSS_XRAWDATA (
  ID BIGINT NOT NULL,
  DATA VARBINARY(1073741824) NOT NULL,
  CHARACTER_ENCODING VARCHAR(50) NULL
);

alter table SS_SYNTAX_MODEL add constraint PK_SYNTAX_MODEL primary key (ID);
alter table SS_XMLSTRUCTURE add constraint PK_XMLSTRUCTURE primary key (ID);
alter table SS_XML_MAPPING add constraint PK_XML_MAPPING primary key (ID);
alter table SS_CSVSTRUCTURE add constraint PK_CSVSTRUCTURE primary key (ID);
alter table SS_CSVSTRUCTURE_FIELD add constraint PK_CSVSTRUCTURE_FIELD primary key (ID);
alter table SS_CSV_MAPPING add constraint PK_CSV_MAPPING primary key (ID);
alter table SS_TEMPLATE add constraint PK_TEMPLATE primary key (ID);
alter table SS_TEMPLATE_CONTENT add constraint PK_TEMPLATE_CONTENT primary key (ID);
alter table SS_DATATYPE add constraint PK_DATATYPE primary key (ID);
alter table SS_TEMPLATE_DATATYPE add constraint PK_TEMPLATE_DATATYPE primary key (ID);
alter table SS_RAWDATA add constraint PK_RAWDATA primary key (ID);
alter table XSS_XSYNTAX_MODEL add constraint PK_XSYNTAX_MODEL primary key (ID);
alter table XSS_XXMLSTRUCTURE add constraint PK_XXMLSTRUCTURE primary key (ID);
alter table XSS_XXML_MAPPING add constraint PK_XXML_MAPPING primary key (ID);
alter table XSS_XCSVSTRUCTURE add constraint PK_XCSVSTRUCTURE primary key (ID);
alter table XSS_XCSVSTRUCTURE_FIELD add constraint PK_XCSVSTRUCTURE_FIELD primary key (ID);
alter table XSS_XCSV_MAPPING add constraint PK_XCSV_MAPPING primary key (ID);
alter table XSS_XTEMPLATE add constraint PK_XTEMPLATE primary key (ID);
alter table XSS_XTEMPLATE_CONTENT add constraint PK_XTEMPLATE_CONTENT primary key (ID);
alter table XSS_XDATATYPE add constraint PK_XDATATYPE primary key (ID);
alter table XSS_XTEMPLATE_DATATYPE add constraint PK_XTEMPLATE_DATATYPE primary key (ID);
alter table XSS_XRAWDATA add constraint PK_XRAWDATA primary key (ID);

alter table SS_SYNTAX_MODEL add constraint FK_SYNTAX_MODEL_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table SS_SYNTAX_MODEL add constraint FK_SYNTAX_MODEL_USER foreign key (USER_ID) references ACL_USER(ID);
alter table SS_XMLSTRUCTURE add constraint FK_XMLSTRUCTURE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table SS_XML_MAPPING add constraint FK_XML_MAPPING_SYNTAX_MODEL foreign key (SYNTAX_MODEL_ID) references SS_SYNTAX_MODEL(ID);
alter table SS_XML_MAPPING add constraint FK_XML_MAPPING_SUBSYNTAX_MODEL foreign key (SUB_SYNTAX_ID) references SS_SYNTAX_MODEL(ID);
alter table SS_CSVSTRUCTURE add constraint FK_CSVSTRUCTURE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table SS_CSVSTRUCTURE_FIELD add constraint FK_CSVSTRUCTURE_FIELD_CSVSTRUCTURE foreign key (CSVSTRUCTURE_ID) references SS_CSVSTRUCTURE(ID);
alter table SS_CSV_MAPPING add constraint FK_CSV_MAPPING_SYNTAX_MODEL foreign key (SYNTAX_MODEL_ID) references SS_SYNTAX_MODEL(ID);
alter table SS_CSV_MAPPING add constraint FK_CSV_MAPPING_CSVSTRUCTURE_FIELD foreign key (CSVSTRUCTURE_FIELD_ID) references SS_CSVSTRUCTURE_FIELD(ID);
alter table SS_TEMPLATE add constraint FK_TEMPLATE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table SS_TEMPLATE_CONTENT add constraint FK_TEMPLATE_CONTENT_TEMPLATE foreign key (TEMPLATE_ID) references SS_TEMPLATE(ID);
alter table SS_DATATYPE add constraint FK_DATATYPE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table SS_TEMPLATE_DATATYPE add constraint FK_TEMPLATE_DATATYPE_TEMPLATE foreign key (TEMPLATE_ID) references SS_TEMPLATE(ID);
alter table SS_TEMPLATE_DATATYPE add constraint FK_TEMPLATE_DATATYPE_DATATYPE foreign key (DATATYPE_ID) references SS_DATATYPE(ID);
alter table XSS_XSYNTAX_MODEL add constraint FK_XSYNTAX_MODEL_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table XSS_XSYNTAX_MODEL add constraint FK_XSYNTAX_MODEL_USER foreign key (USER_ID) references ACL_USER(ID);
alter table XSS_XXMLSTRUCTURE add constraint FK_XXMLSTRUCTURE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table XSS_XXML_MAPPING add constraint FK_XXML_MAPPING_XSYNTAX_MODEL foreign key (XSYNTAX_MODEL_ID) references XSS_XSYNTAX_MODEL(ID);
alter table XSS_XXML_MAPPING add constraint FK_CXML_MAPPING_SUBSYNTAX_MODEL foreign key (SUB_SYNTAX_ID) references XSS_XSYNTAX_MODEL(ID);
alter table XSS_XCSVSTRUCTURE add constraint FK_XCSVSTRUCTURE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table XSS_XCSVSTRUCTURE_FIELD add constraint FK_XCSVSTRUCTURE_FIELD_XCSVSTRUCTURE foreign key (XCSVSTRUCTURE_ID) references XSS_XCSVSTRUCTURE(ID);
alter table XSS_XCSV_MAPPING add constraint FK_XCSV_MAPPING_XSYNTAX_MODEL foreign key (XSYNTAX_MODEL_ID) references XSS_XSYNTAX_MODEL(ID);
alter table XSS_XCSV_MAPPING add constraint FK_XCSV_MAPPING_XCSVSTRUCTURE_FIELD foreign key (XCSVSTRUCTURE_FIELD_ID) references XSS_XCSVSTRUCTURE_FIELD(ID);
alter table XSS_XTEMPLATE add constraint FK_XTEMPLATE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table XSS_XTEMPLATE_CONTENT add constraint FK_XTEMPLATE_CONTENT_XTEMPLATE foreign key (XTEMPLATE_ID) references XSS_XTEMPLATE(ID);
alter table XSS_XDATATYPE add constraint FK_XDATATYPE_ACCESS_AREA foreign key (ACCESS_AREA_ID) references ACL_ACCESS_AREA(ID);
alter table XSS_XTEMPLATE_DATATYPE add constraint FK_XTEMPLATE_DATATYPE_XTEMPLATE foreign key (XTEMPLATE_ID) references XSS_XTEMPLATE(ID);
alter table XSS_XTEMPLATE_DATATYPE add constraint FK_XTEMPLATE_DATATYPE_XDATATYPE foreign key (XDATATYPE_ID) references XSS_XDATATYPE(ID);

alter table SS_SYNTAX_MODEL add constraint UC_SYNTAX_MODEL_1 unique (NAME,ACCESS_AREA_ID);
alter table SS_XMLSTRUCTURE add constraint UC_XMLSTRUCTURE_1 unique (NAME,ACCESS_AREA_ID);
alter table SS_CSVSTRUCTURE add constraint UC_CSVSTRUCTURE_1 unique (NAME,ACCESS_AREA_ID);
alter table SS_TEMPLATE add constraint UC_TEMPLATE_1 unique (NAME,ACCESS_AREA_ID);
alter table SS_DATATYPE add constraint UC_DATATYPE_1 unique (NAME,ACCESS_AREA_ID);
alter table XSS_XSYNTAX_MODEL add constraint UC_XSYNTAX_MODEL_1 unique (NAME,ACCESS_AREA_ID);
alter table XSS_XXMLSTRUCTURE add constraint UC_XXMLSTRUCTURE_1 unique (NAME,ACCESS_AREA_ID);
alter table XSS_XCSVSTRUCTURE add constraint UC_XCSVSTRUCTURE_1 unique (NAME,ACCESS_AREA_ID);
alter table XSS_XTEMPLATE add constraint UC_XTEMPLATE_1 unique (NAME,ACCESS_AREA_ID);
alter table XSS_XDATATYPE add constraint UC_XDATATYPE_1 unique (NAME,ACCESS_AREA_ID);