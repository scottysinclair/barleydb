package scott.sort.test.remoteclient;

import java.util.List;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import scott.sort.api.core.QueryBatcher;

import com.smartstream.mac.query.QUser;
import com.smartstream.mi.model.SyntaxModel;
import com.smartstream.mi.model.SyntaxType;
import com.smartstream.mi.model.Template;
import com.smartstream.mi.model.XMLSyntaxModel;
import com.smartstream.mi.query.QSyntaxModel;
import com.smartstream.mi.query.QTemplate;
import com.smartstream.mi.query.QXMLMapping;
import com.smartstream.mi.query.QXMLSyntaxModel;

public class TestRemoteClientQuery extends TestRemoteClientBase {

    @Override
    protected void prepareData() {
        super.prepareData();
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/inserts.sql"), false);
    }

    /**
     * Loads all ROOT syntaxes in one abstract query (XML + CSV)
     * Only concrete proxies are instantiated (XML + CSV) as the base proxy is
     * an abstract class which fundamentally cannot be instantiated.
     * @throws Exception
     */
    @Test
    public void testBaseSyntaxModels() throws Exception {
        QSyntaxModel qsyntax = new QSyntaxModel();
        qsyntax.where(qsyntax.syntaxType().equal(SyntaxType.ROOT));
        qsyntax.joinToUser();

        List<SyntaxModel> syntaxModels = clientEntityContext.performQuery(qsyntax).getList();
        for (SyntaxModel syntaxModel : syntaxModels) {
            syntaxModel.getStructure().getName();
            System.out.println(syntaxModel.getName() + " -- " + syntaxModel.getUser().getName() + " -- " + syntaxModel.getStructure().getName());
        }
        for (SyntaxModel syntaxModel : syntaxModels) {
            print("", syntaxModel);
        }
    }

    @Test
    public void testBatchQuery() throws Exception {
        /*
         * Build a syntax model query
         */
        QXMLSyntaxModel syntax = (QXMLSyntaxModel) clientEntityContext.getDefinitions().getQuery(XMLSyntaxModel.class);
        QXMLMapping aMapping = syntax.existsMapping();
        QUser aUser = syntax.existsUser();
        syntax.where(syntax.syntaxName().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.userName().equal("Scott")));

        /*
         * Build a template query
         */
        QTemplate templatesQuery = new QTemplate();
        templatesQuery.joinToDatatype();

        QueryBatcher qBatch = new QueryBatcher();
        qBatch.addQuery(syntax, templatesQuery);

        clientEntityContext.performQueries(qBatch);

        System.out.println();
        System.out.println();
        System.out.println("Printing Syntax models");

        for (XMLSyntaxModel syntaxModel : qBatch.getResult(0, XMLSyntaxModel.class).getList()) {
            print("", syntaxModel);
        }

        System.out.println();
        System.out.println("Printing Templates");

        for (Template template : qBatch.getResult(1, Template.class).getList()) {
            print("", template);
        }
    }

}
