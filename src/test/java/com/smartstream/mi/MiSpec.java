package com.smartstream.mi;

import static scott.sort.api.specification.CoreSpec.dependsOn;
import static scott.sort.api.specification.CoreSpec.mandatoryEnum;
import static scott.sort.api.specification.CoreSpec.mandatoryFixedEnum;
import static scott.sort.api.specification.CoreSpec.mandatoryRefersTo;
import static scott.sort.api.specification.CoreSpec.optionalRefersTo;
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
        super("com.smartstream.messaging");
        add(new MacSpec());
        excludeForeignKeyConstraint(new Relation(XmlSyntax.class, XmlStructure.class));
        excludeForeignKeyConstraint(new Relation(CsvSyntax.class, CsvStructure.class));
    }


    @Override
    public Class<?>[] getOrder() {
        return new Class[]{
                Syntax.class,
                XmlSyntax.class,
                XmlStructure.class,
                XmlMapping.class,
                CsvSyntax.class,
                CsvStructure.class,
                CsvStructureField.class,
                CsvMapping.class,
                Template.class,
                BusinessType.class,
                TemplateBusinessType.class
        };
    }

    @AbstractEntity("MMI_SYNTAX")
    public static class Syntax implements TopLevelModel {

        public static final NodeSpec structureType = mandatoryEnum(StructureType.class);

        public static final NodeSpec syntaxType = mandatoryEnum(SyntaxType.class);

        public static final NodeSpec user = optionalRefersTo(User.class);

        public static final NodeSpec structure = mandatoryLongValue("STRUCTURE_ID");
    }

    @Entity("MMI_XML_STRUCTURE")
    public static class XmlStructure implements TopLevelModel {
    }

    @ExtendsEntity
    public static class XmlSyntax extends Syntax {

        public static final NodeSpec structureType = mandatoryFixedEnum(StructureType.XML);

        public static final NodeSpec structure = dependsOn(XmlStructure.class, "STRUCTURE_ID");

        public static final NodeSpec mappings = ownsMany(XmlMapping.class, XmlMapping.syntax);
    }

    @Entity("MMI_XML_MAPPING")
    public static class XmlMapping {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec syntax = mandatoryRefersTo(XmlSyntax.class);

        public static final NodeSpec subSyntax = optionallyOwns(XmlSyntax.class, "SUB_SYNTAX_ID");

        public static final NodeSpec xpath = mandatoryVarchar50();
    }

    @ExtendsEntity
    public static class CsvSyntax extends Syntax {

        public static final NodeSpec structureType = mandatoryFixedEnum(StructureType.CSV);

        public static final NodeSpec structure = dependsOn(CsvStructure.class, "STRUCTURE_ID");

        public static final NodeSpec mappings = ownsMany(CsvMapping.class, CsvMapping.syntax);
    }

    @Entity("MMI_CSV_STRUCTURE")
    public static class CsvStructure implements TopLevelModel {

        public static final NodeSpec headerBasedMapping = mandatoryBooleanValue();

        public static final NodeSpec fields = ownsMany(CsvStructureField.class, CsvStructureField.structure);
    }

    @Entity("MMI_CSV_STRUCTURE_FIELD")
    public static class CsvStructureField {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec structure = mandatoryRefersTo(CsvStructure.class);

        public static final NodeSpec columnIndex = mandatoryIntegerValue();

        public static final NodeSpec optional = mandatoryBooleanValue();

    }

    @Entity("MMI_CSV_MAPPING")
    public static class CsvMapping {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec syntax = mandatoryRefersTo(CsvSyntax.class);

        public static final NodeSpec structureField = mandatoryRefersTo(CsvStructureField.class);
    }

    @Entity("MMI_TEMPLATE")
    public static class Template implements TopLevelModel {

        public static final NodeSpec businessTypes = ownsMany(TemplateBusinessType.class, TemplateBusinessType.template);

    }

    @Entity("MMI_TEMPLATE_TEMPLATE_DATATYPE")
    public static class TemplateBusinessType {

        public static final NodeSpec template = mandatoryRefersTo(Template.class);

        public static final NodeSpec businessType = mandatoryRefersTo(BusinessType.class);

    }

    @Entity("MMI_TEMPLATE_DATATYPE")
    public static class BusinessType implements TopLevelModel {
    }

}