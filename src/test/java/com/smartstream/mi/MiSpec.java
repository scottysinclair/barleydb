package com.smartstream.mi;

import static scott.sort.api.specification.CoreSpec.dependsOn;
import static scott.sort.api.specification.CoreSpec.mandatoryRefersTo;
import static scott.sort.api.specification.CoreSpec.optionallyRefersTo;
import static scott.sort.api.specification.CoreSpec.optionallyOwns;
import static scott.sort.api.specification.CoreSpec.ownsMany;

import com.smartstream.MorpheusSpec;
import com.smartstream.mac.MacSpec;
import com.smartstream.mac.MacSpec.TopLevelModel;
import com.smartstream.mac.MacSpec.User;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.types.SyntaxType;

import scott.sort.api.specification.NodeSpec;
import scott.sort.build.specification.staticspec.AbstractEntity;
import scott.sort.build.specification.staticspec.Entity;
import scott.sort.build.specification.staticspec.ExtendsEntity;
import scott.sort.build.specification.staticspec.SuppressFromGeneratedCode;

/**
 * interfaces define common groupings of NodeSpecs
 *
 * sub-classing defines inheritance in the domain model
 *
 *  These static definitions can be used to generate:
 *  <ul>
 *   <li>Model classes in the source code tree</li>
 *   <li>Query classes in the source code tree</li>
 *   <li>XML files to load on application startup.</li>
 *  </ul>
 *
 */

public class MiSpec extends MorpheusSpec {

    public MiSpec() {
        super("com.smartstream.mi");
        add(new MacSpec());
        excludeForeignKeyConstraint(new StaticRelation(XmlSyntaxModel.structure));
        excludeForeignKeyConstraint(new StaticRelation(CsvSyntaxModel.structure));
        renameForeignKeyConstraint(new StaticRelation(XmlMapping.subSyntax), "FK_XML_MAPPING_SUBSYNTAX_MODEL");
    }

    @Override
    public Class<?>[] getOrder() {
        return new Class[]{
                SyntaxModel.class,
                XmlSyntaxModel.class,
                XmlStructure.class,
                XmlMapping.class,
                CsvSyntaxModel.class,
                CsvStructure.class,
                CsvStructureField.class,
                CsvMapping.class,
                Template.class,
                TemplateContent.class,
                BusinessType.class,
                TemplateBusinessType.class,
                RawData.class
        };
    }

    @AbstractEntity("SS_SYNTAX_MODEL")
    public static class SyntaxModel implements TopLevelModel {

        public static final NodeSpec structureType = mandatoryEnum(StructureType.class);

        public static final NodeSpec syntaxType = mandatoryEnum(SyntaxType.class);

        public static final NodeSpec user = optionallyRefersTo(User.class);

        @SuppressFromGeneratedCode
        public static final NodeSpec structure = mandatoryLongValue("STRUCTURE_ID");
    }

    @Entity("SS_XMLSTRUCTURE")
    public static class XmlStructure implements TopLevelModel {
    }

    @ExtendsEntity
    public static class XmlSyntaxModel extends SyntaxModel {

        public static final NodeSpec structureType = mandatoryFixedEnum(StructureType.XML);

        public static final NodeSpec structure = dependsOn(XmlStructure.class, "STRUCTURE_ID");

        public static final NodeSpec mappings = ownsMany(XmlMapping.class, XmlMapping.syntax);
    }

    @Entity("SS_XML_MAPPING")
    public static class XmlMapping {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec syntax = mandatoryRefersTo(XmlSyntaxModel.class);

        public static final NodeSpec subSyntax = optionallyOwns(XmlSyntaxModel.class, "SUB_SYNTAX_ID");

        public static final NodeSpec xpath = mandatoryVarchar150();

        public static final NodeSpec targetFieldName = mandatoryVarchar150();
    }

    @ExtendsEntity
    public static class CsvSyntaxModel extends SyntaxModel {

        public static final NodeSpec structureType = mandatoryFixedEnum(StructureType.CSV);

        public static final NodeSpec structure = dependsOn(CsvStructure.class, "STRUCTURE_ID");

        public static final NodeSpec mappings = ownsMany(CsvMapping.class, CsvMapping.syntax);
    }

    @Entity("SS_CSVSTRUCTURE")
    public static class CsvStructure implements TopLevelModel {

        public static final NodeSpec headerBasedMapping = mandatoryBooleanValue();

        public static final NodeSpec fields = ownsMany(CsvStructureField.class, CsvStructureField.structure);
    }

    @Entity("SS_CSVSTRUCTURE_FIELD")
    public static class CsvStructureField {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec name = optionalVarchar50();

        public static final NodeSpec structure = mandatoryRefersTo(CsvStructure.class);

        public static final NodeSpec columnIndex = mandatoryIntegerValue();

        public static final NodeSpec optional = mandatoryBooleanValue();

    }

    @Entity("SS_CSV_MAPPING")
    public static class CsvMapping {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec syntax = mandatoryRefersTo(CsvSyntaxModel.class);

        public static final NodeSpec structureField = mandatoryRefersTo(CsvStructureField.class);

        public static final NodeSpec targetFieldName = mandatoryVarchar150();
    }

    @Entity("SS_TEMPLATE")
    public static class Template implements TopLevelModel {

        public static final NodeSpec contents = ownsMany(TemplateContent.class, TemplateContent.template);

        public static final NodeSpec businessTypes = ownsMany(TemplateBusinessType.class, TemplateBusinessType.template, TemplateBusinessType.businessType);
    }

    @Entity("SS_TEMPLATE_CONTENT")
    public static class TemplateContent {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec name = name();

        public static final NodeSpec modifiedAt = optimisticLock();

        public static final NodeSpec template = mandatoryRefersTo(Template.class);
    }

    @Entity("SS_TEMPLATE_DATATYPE")
    public static class TemplateBusinessType {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec template = mandatoryRefersTo(Template.class);

        public static final NodeSpec businessType = mandatoryRefersTo(BusinessType.class);
    }

    @Entity("SS_DATATYPE")
    public static class BusinessType implements TopLevelModel {
    }

    @Entity("SS_RAWDATA")
    public static class RawData {
        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec data = mandatoryNonStreamingBlob();

        public static final NodeSpec characterEncoding = optionalVarchar50();
    }

}