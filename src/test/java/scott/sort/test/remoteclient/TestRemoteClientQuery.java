package scott.sort.test.remoteclient;

import java.util.List;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import com.smartstream.mi.model.SyntaxModel;
import com.smartstream.mi.model.SyntaxType;
import com.smartstream.mi.query.QSyntaxModel;

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
}
