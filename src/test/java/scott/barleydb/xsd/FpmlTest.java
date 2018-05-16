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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import scott.barleydb.xsd.exception.XsdDefinitionException;


@Ignore
public class FpmlTest extends ParentTest {

    public static final String importPath = "schemas/fpml";
    private static XsdDefinition tradeCapture;

    @Before
    public void before() throws Exception {
        tradeCapture = loadDefinition(importPath + "/fixml-tradecapture-impl-5-0-SP2.xsd");
    }

    /**
     * the element walker terminates and does not include abstract methods
     * and includes substituion groups
     */
    private int count = 0;
    @Test
    @Ignore
    public void testElementWalker() throws Exception {
        XsdElementWalker walker = new XsdElementWalker(tradeCapture, true, true, true);
        long start = System.currentTimeMillis();
        printChildElements("", walker);
        System.out.println("walked " + count + " elements, took " + ((System.currentTimeMillis() - start) / 1000.0) + " secs");
    }

    private void printChildElements(String prefix, XsdElementWalker walker) throws XsdDefinitionException {
        for (XsdElementWalker child: walker.getChildren()) {
            count++;
            //System.out.println(prefix + child.getElement().getElementName());
            printChildElements(prefix + " ", child);
        }
    }

    @Test
    public void testNoDuplicatePossibleRoots() throws Exception {
        Set<String> elements = new HashSet<>();
        XsdElementWalker walker = new XsdElementWalker(tradeCapture);
        for (XsdElementWalker child: walker.getChildren()) {
            if (!elements.add(child.getElement().getElementName() + child.getElement().getTypeName())) {
                Assert.fail(child.getElement().getElementName() + " is duplicated");
            }
        }
    }


    @Test
    public void testFpmlGap1() throws Exception {
        XsdElement trdCaptRpt = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "TrdCaptRpt"));
        assertEquals(134, trdCaptRpt.getAttributes().size());
    }

    @Test
    public void testFpmlGap2() throws Exception {
        XsdComplexType prevTradeDt = (XsdComplexType) tradeCapture.findType(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "PrevTradeDt_Block_t"));
        assertNotNull(prevTradeDt);
        assertEquals(1, prevTradeDt.getAttributes().size());

        XsdGroup group = tradeCapture.findGroup( new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "TradeCaptureReportElements") );
        XsdElement element =  group.getElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "PrevTradeDt"));
        assertNotNull(element);
        assertEquals(1, element.getAttributes().size());
    }



    @Test
    public void testSubstitutionGroups() throws Exception {
        /*
         * The expected substitution elements came from the following command executed on the FPML XSD folder.
         *
         * grep substitutionGroup * | grep -i ELEMENT_NAME| cut --delimiter='"' -f 2
         */
        XsdElement element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "Message"));
        List<XsdElement> subs = element.getSubstitutions();
        assertElements(subs,
                "TrdCaptRptReq",
                "TrdCaptRpt",
                "TrdCaptRptReqAck",
                "TrdCaptRptAck");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "underlyingAsset"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "basket",
                "bond",
                "cash",
                "commodity",
                "convertibleBond",
                "equity",
                "exchangeTradedFund",
                "future",
                "index",
                "loan",
                "mortgage",
                "mutualFund",
                "option");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "curveInstrument"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "deposit",
                "fx",
                "rateIndex",
                "simpleCreditDefaultSwap",
                "simpleFra",
                "simpleIrSwap");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "product"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "bondOption",
                "creditDefaultSwap",
                "creditDefaultSwapOption",
                "commodityBasketOption",
                "commodityDigitalOption",
                "commodityForward",
                "commodityOption",
                "commodityPerformanceSwap",
                "commoditySwap",
                "commoditySwaption",
                "correlationSwap",
                "dividendSwapOptionTransactionSupplement",
                "dividendSwapTransactionSupplement",
                "instrumentTradeDetails",
                "strategy",
                "brokerEquityOption",
                "equityForward",
                "equityOption",
                "equityOptionTransactionSupplement",
                "returnSwap",
                "fxTargetForward",
                "fxDigitalOption",
                "fxFlexibleForward",
                "fxForwardVolatilityAgreement",
                "fxSingleLeg",
                "fxSwap",
                "fxOption",
                "fxVarianceSwap",
                "fxVolatilitySwap",
                "termDeposit",
                "genericProduct",
                "nonSchemaProduct",
                "bulletPayment",
                "capFloor",
                "fra",
                "swap",
                "swaption",
                "repo",
                "equitySwapTransactionSupplement",
                "standardProduct",
                "varianceOptionTransactionSupplement",
                "varianceSwap",
                "varianceSwapTransactionSupplement");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "changeEvent"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "basketChange",
                "corporateAction",
                "indexChange");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "commodityPerformanceSwapLeg"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "commodityInterestLeg",
                "commodityReturnLeg",
                "commodityVarianceLeg");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "commodityForwardLeg"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "bullionPhysicalLeg",
                "metalPhysicalLeg");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "commoditySwapLeg"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "coalPhysicalLeg",
                "electricityPhysicalLeg",
                "environmentalPhysicalLeg",
                "fixedLeg",
                "floatingLeg",
                "oilPhysicalLeg",
                "gasPhysicalLeg");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "creditEvent"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "bankruptcy",
                "failureToPay",
                "obligationAcceleration",
                "obligationDefault",
                "repudiationMoratorium",
                "restructuring");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "returnSwapLeg"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "interestLeg",
                "returnLeg");


        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "fxDisruptionEvent"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "dualExchangeRate",
                "exchangeRestrictions",
                "priceSourceDisruption",
                "priceMateriality");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "fxDisruptionFallback"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "calculationAgentDetermination",
                "fallbackReferencePrice",
                "noFaultTermination",
                "nonDeliverableSubstitute",
                "settlementPostponement",
                "valuationPostponement");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "rateCalculation"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "floatingRateCalculation",
                "inflationRateCalculation");


        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "contractualDocument"));
        subs = element.getSubstitutions();
        //standardCsa is itself a substitution group which standardCsa2013EnglishLaw and standardCsa2013NewYorkLaw belong to
        assertElements(subs, "standardCsa", "standardCsa2013EnglishLaw", "standardCsa2013NewYorkLaw");

        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "pricingStructure"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "creditCurve",
                "fxCurve",
                "volatilityRepresentation",
                "yieldCurve");


        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "pricingStructureValuation"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "creditCurveValuation",
                "fxCurveValuation",
                "volatilityMatrixValuation",
                "yieldCurveValuation");


        element = tradeCapture.findElement(new QualifiedName("http://svn.msk.trd.ru/xsd/fixml", "exercise"));
        subs = element.getSubstitutions();
        assertElements(subs,
                "americanExercise",
                "bermudaExercise",
                "europeanExercise");

    }

    private void assertElements(List<XsdElement> coll, String ...names) throws XsdDefinitionException {
        java.util.Map<String,Integer> expectedNames = new HashMap<String,Integer>();
        for (int i=0; i<names.length; i++) {
            Integer count = expectedNames.get(names[i]);
            if (count == null) count = 0;
            expectedNames.put(names[i], ++count);
        }
        for (XsdElement el: coll) {
            Integer count = expectedNames.get(el.getElementName());
            //System.out.println(el.getElementName() + " " + count);
            assertNotNull(el.getElementName() + " not expected", count);
            expectedNames.put(el.getElementName(), --count);
        }
        for (java.util.Map.Entry<String, Integer> e: expectedNames.entrySet()) {
//            System.out.println(e.getKey() + " " + e.getValue());
            assertTrue(e.getKey() + " is missing", e.getValue() == 0);
        }
    }

    private void assertAttributes(List<XsdAttribute> coll, String ...names) throws XsdDefinitionException {
        java.util.Map<String,Integer> expectedNames = new HashMap<String,Integer>();
        for (int i=0; i<names.length; i++) {
            Integer count = expectedNames.get(names[i]);
            if (count == null) count = 0;
            expectedNames.put(names[i], ++count);
        }
        for (XsdAttribute attr: coll) {
            Integer count = expectedNames.get(attr.getName());
            //System.out.println(el.getElementName() + " " + count);
            assertNotNull(attr.getName() + " not expected", count);
            expectedNames.put(attr.getName(), --count);
        }
        for (java.util.Map.Entry<String, Integer> e: expectedNames.entrySet()) {
//            System.out.println(e.getKey() + " " + e.getValue());
            assertTrue(e.getKey() + " is missing", e.getValue() == 0);
        }
    }

    private void printChildElements(String prefix, XsdNode node, Set<String> alreadyProcessed ) throws XsdDefinitionException {
        for (XsdElement element: node.getChildTargetElements()) {
            String key = element.getElementNamespace() + "." + element.getElementName();
            if (alreadyProcessed.add(key)) {
                printElement(prefix, element, alreadyProcessed);
            }
        }
    }

    private void printElement(String prefix, XsdElement element, Set<String> alreadyProcessed) throws XsdDefinitionException {
        System.out.println(prefix +  element.getElementName());
        for (XsdAttribute attr: element.getAttributes()) {
            System.out.println(prefix + "@" +  attr.getName());
        }
        printChildElements(prefix + " ", element, alreadyProcessed);
        for (XsdElement sub: element.getSubstitutions()) {
            printElement(prefix + "SUB of " + element.getElementName() + " ", sub, alreadyProcessed);
        }
    }
}
