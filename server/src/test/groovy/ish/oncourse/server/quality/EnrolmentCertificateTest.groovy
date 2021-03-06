package ish.oncourse.server.quality

import ish.oncourse.server.cayenne.QualityRule
import org.apache.cayenne.query.Select
import org.junit.Test
import static org.mockito.Matchers.any
import static org.mockito.Mockito.when

/**
 * Created by akoiro on 17/03/2016.
 */
class EnrolmentCertificateTest {
    @Test
    void test() {

        ScriptTestService testService = new ScriptTestService()
        OutcomeResultData outcomeResultData = new OutcomeResultData()

        QualityService qualityService = testService.qualityService

        def data = outcomeResultData.testData()
        when(testService.context.select(any(Select))).thenReturn(data)

        QualityRule qualityRule = testService.getQualityRuleBy("enrolmentCertificate")

        def result = qualityService.performRuleCheck(qualityRule)
        outcomeResultData.assetResults(result)
    }
}
