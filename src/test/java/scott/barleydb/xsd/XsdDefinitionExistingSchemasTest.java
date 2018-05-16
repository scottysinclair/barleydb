package scott.barleydb.xsd;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
 *       <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import scott.barleydb.xsd.QualifiedName;
import scott.barleydb.xsd.XsdComplexType;
import scott.barleydb.xsd.XsdDefinition;
import scott.barleydb.xsd.XsdElement;

public class XsdDefinitionExistingSchemasTest extends ParentTest {

    XsdDefinition xsdDefinitionFxRate;
    XsdDefinition assetImport;
    XsdDefinition accountImportTypes;
    XsdDefinition fin940;
    XsdDefinition xsdA;

    @Before
    public void before() throws Exception {
        xsdDefinitionFxRate = loadDefinition("schemas/hsbc/MODPDataMessage_FXRate_v0.3.xsd");
        assetImport = loadDefinition("schemas/asset_import-2.0.xsd");
        accountImportTypes = loadDefinition("schemas/AccountImport_Types_allonefileformi_refactoredentitie.xsd");
        fin940 = loadDefinition("schemas/fin.940.2011.xsd");
        xsdA = loadDefinition("schemas/loading/TestXsdA.xsd");
    }

    @Test
    public void  testGetNamespace() {
        assertNull(xsdDefinitionFxRate.getTargetNamespace());
        assertNull(assetImport.getTargetNamespace());
        assertNull(accountImportTypes.getTargetNamespace());
        assertEquals("urn:swift:xsd:fin.940.2011", fin940.getTargetNamespace());
        assertEquals("urn:a", xsdA.getTargetNamespace());
    }

    @Test
    public void testGetRootTargetElements() throws Exception {
        assertElementNames(Arrays.asList("MODPDataMessage"), xsdDefinitionFxRate.getChildTargetElements());
        assertElementNames(execpectedAssetImportRootElements(), assetImport.getChildTargetElements());
        assertElementNames(Arrays.asList("AccountImports"), accountImportTypes.getChildTargetElements());
        assertElementNames(Arrays.asList("Document"), fin940.getChildTargetElements());
    }

    @Test
    public void testElementsAreQualifiedByDefault() {
        assertTrue("Elements should be qualified by default", xsdDefinitionFxRate.elementsAreQualifiedByDefault() );
        assertTrue("Elements should NOT be qualified by default", !assetImport.elementsAreQualifiedByDefault() );
        assertTrue("Elements should NOT be qualified by default", !accountImportTypes.elementsAreQualifiedByDefault() );
        assertTrue("Elements should be qualified by default", fin940.elementsAreQualifiedByDefault() );
        assertTrue("Elements should be qualified by default", xsdA.elementsAreQualifiedByDefault() );
    }

    @Test
    public void testIncludes() {
        assertSchemaLocations(Arrays.asList("schemas/hsbc/MODPDataRecordHeader_v0.1.xsd", "schemas/hsbc/MODPData_FXRate_v0.4.xsd"), xsdDefinitionFxRate.getIncludes());
        assertTrue("unexpected schema includes", assetImport.getIncludes().isEmpty());
        assertTrue("unexpected schema includes", accountImportTypes.getIncludes().isEmpty());
        assertTrue("unexpected schema includes", fin940.getIncludes().isEmpty());
    }

    @Test
    public void testImports() {
        assertTrue("unexpected schema imports", xsdDefinitionFxRate.getImports().isEmpty());
        assertTrue("unexpected schema imports", assetImport.getImports().isEmpty());
        assertTrue("unexpected schema imports", accountImportTypes.getImports().isEmpty());
        assertTrue("unexpected schema imports", fin940.getImports().isEmpty());
        assertTrue("expected 1 schema import", xsdA.getImports().size() == 1);
    }

    @Test
    public void testElementWithComplexTypeInOtherNamespace() throws Exception {
        XsdElement a_person = null;
        assertTrue(
                (a_person = xsdA.findElement(
                        new QualifiedName(xsdA,"a:person")))!=null);
        assertEquals("b:person",
                a_person.getTypeName());
        XsdComplexType type = (XsdComplexType) xsdA.findType(
                new QualifiedName(xsdA,
                a_person.getTypeName()));
        assertNotNull(type);
        assertEquals("urn:b",type.getXsdDefinition().getTargetNamespace());
    }

    private List<String> execpectedAssetImportRootElements() {
        return Arrays.asList(
                "TRADEABLE_LOT",
                "PAYMENT_FREQUENCY",
                "PAR_VALUE",
                "BUSINESS_UNIT",
                "CLIENT",
                "LISTING_ASSET_PRICE",
                "LISTING_MARKET_VALUE",
                "LISTING_OF_INTEREST",
                "LISTING_ACTIVE_STATUS",
                "LISTING_START_DATE",
                "LISTING_END_DATE",
                "LISTING_MIC_ISO_CODE",
                "LISTING_SETTLEMENT_COUNTRY_ISO_CODE",
                "LISTING_CURRENCY",
                "LISTINGS",
                "LISTING",
                "ISSUER_NAME",
                "ISSUER_DESCRIPTION",
                "ISSUER_COUNTRY_ISO_CODE",
                "ISSUERS",
                "ISSUER",
                "INTEREST_RATE",
                "FIRST_PAYMENT_DATE",
                "DAY_COUNT_CONVENTION",
                "ASSET_TYPE",
                "ASSET_REGISTER",
                "ASSET_REFERENCES",
                "ASSET_REFERENCE",
                "LISTING_REFERENCES",
                "LISTING_REFERENCE",
                "ASSET_NAME",
                "ASSET_MATURITY_DATE",
                "ASSET_START_DATE",
                "ASSET_END_DATE",
                "ASSET_DESCRIPTION",
                "ASSET_ISSUING_COUNTRY_ISO_CODE",
                "ASSET_CURRENCY_ISO_CODE",
                "ASSETS",
                "ASSET",
                "ANNUAL_DIVIDEND_RATE");
    }



}
