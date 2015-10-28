package org.familysearch.paas.utils;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;


/**
 * Unit Tests for ParserHelper Class
 *
 * @author barclays
 */
public class ParserHelperTest {

  private static final String TEST_AWS_ACCOUNT_ID = "000111222333";
  private static final String TEST_ACCOUNT_ALIAS = "my-cool-account";

  @Mock
  private IamHelper mockIamHelper;
  private ParserHelper testModel;
  private String mockSnsEvent;

  @BeforeMethod
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(mockIamHelper.getFirstAccountAlias()).thenReturn(TEST_ACCOUNT_ALIAS);
    testModel = new ParserHelper(mockIamHelper);

    mockSnsEvent = getSnsEventJsonContent();
  }

  private Map<String, String> runJiraCredsTest() throws Exception{
    return testModel.getJiraCreds(CredentialHelperTest.getCredentialJsonContents());
  }

  @Test
  public void itShouldReturnJiraCreds() throws Exception {
    Map<String, String> map = runJiraCredsTest();

    assertEquals(map.get("username"), "myUsername");
  }

  private Map<String, String> runJiraComponentsTest() throws Exception {
    return testModel.getKinesisComponents(mockSnsEvent);
  }

  @Test
  public void itShouldReturnJiraComponentsWithSummary() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    assertNotNull(map.get("summary"));
  }

  @Test
  public void summaryShouldContainTheAccountAlias() throws Exception {
    String validAccountAlias = TEST_ACCOUNT_ALIAS.replace("-", " ");
    Map<String, String> map = runJiraComponentsTest();

    String summary = map.get("summary");
    assertTrue(summary.contains(validAccountAlias), "Expected account alias would be present");
  }

  @Test
  public void itShouldReturnJiraComponentsWithProject() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    assertEquals(map.get("project"), "DPT");
  }

  @Test
  public void itShouldRemoveSpecialCharactersFromSummarySearch() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    String specialCharacters = "[+&|!(){}^~*?\\:\\[\\]-]";
    Pattern matcher = Pattern.compile(specialCharacters);
    assertFalse(matcher.matcher(map.get("summary_search")).find());
  }

  @Test
  public void itShouldReturnAwsAccountId() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    assertEquals(map.get("aws_account_id"), TEST_AWS_ACCOUNT_ID);
  }

  @Test
  public void itShouldReturnDescriptionWithAlarmLink() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    String description = map.get("description");
    String expectedToContain = "https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#alarm:alarmFilter=ANY;name=paas%20sps%20s3%20Check%20WorkflowsTimedOut";
    String message = String.format("Alarm URL is wrong.  Expected: '%s'\nto contain: '%s'", description, expectedToContain);
    assertTrue(description.contains(expectedToContain), message);
  }

  @Test
  public void itShouldReturnDescriptionWithRunbookLink() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    String description = map.get("description");
    assertTrue(description.contains("Runbook Link"));
  }

  @Test
  public void itShouldReturnDescriptionWithReason() throws Exception {
    Map<String, String> map = runJiraComponentsTest();

    String description = map.get("description");
    assertTrue(description.contains("Reason"));
  }

  @Test(expectedExceptions = ParserHelperException.class)
  public void itShouldThrowParserHelperExceptionWhenMissingRecordOrSnsJSON() throws Exception {
    mockSnsEvent = "{\"key1\":\"value1\"}";

    runJiraComponentsTest();
  }

  @Test(expectedExceptions = ParserHelperException.class)
  public void itShouldThrowParserHelperExceptionWhenMalformedMessageJSON() throws Exception {
    mockSnsEvent = "{\"Records\":[{\"EventSource\":\"aws:sns\",\"EventVersion\":\"1.0\",\"EventSubscriptionArn\":\"arn:aws:sns:us-east-1:"+TEST_AWS_ACCOUNT_ID+":barclays-test-topic:1a1c3a30-271d-4a73-a840-97a26d3a0581\",\"Sns\":{\"Type\":\"Notification\",\"MessageId\":\"55e9f511-b020-57bd-be60-b8c5ebfe07d7\",\"TopicArn\":\"arn:aws:sns:us-east-1:"+TEST_AWS_ACCOUNT_ID+":barclays-test-topic\",\"Subject\":\"ALARM: \\\"paas-sps-s3-check-completed\\\" in US - N. Virginia\",\"Message\":\"{\\\"AlarmName\\\":\\\"test\\\",\\\"AlarmDescription\\\":\\\"{\\\\\\\"project\\\\\\\":\\\\\\\"DPT\\\\\\\",\\\\\\\"component\\\\\\\";\\\\\\\"Platform\\\\\\\",\\\\\\\"priority\\\\\\\":\\\\\\\"3\\\\\\\",\\\\\\\"recipient\\\\\\\":\\\\\\\"test\\\\\\\"}\\\",\\\"AWSAccountId\\\":\\\""+TEST_AWS_ACCOUNT_ID+"\\\",\\\"NewStateValue\\\":\\\"ALARM\\\",\\\"NewStateReason\\\":\\\"Threshold Crossed: 1 datapoint (1.0) was greater than or equal to the threshold (0.0).\\\",\\\"StateChangeTime\\\":\\\"2015-05-18T21:29:02.273+0000\\\",\\\"Region\\\":\\\"US - N. Virginia\\\",\\\"OldStateValue\\\":\\\"OK\\\",\\\"Trigger\\\":{\\\"MetricName\\\":\\\"WorkflowsCompleted\\\",\\\"Namespace\\\":\\\"AWS/SWF\\\",\\\"Statistic\\\":\\\"SUM\\\",\\\"Unit\\\":null,\\\"Dimensions\\\":[{\\\"name\\\":\\\"WorkflowTypeVersion\\\",\\\"value\\\":\\\"0.0.1\\\"},{\\\"name\\\":\\\"Domain\\\",\\\"value\\\":\\\"paas-sps\\\"},{\\\"name\\\":\\\"WorkflowTypeName\\\",\\\"value\\\":\\\"S3Provisioner.check\\\"}],\\\"Period\\\":60,\\\"EvaluationPeriods\\\":1,\\\"ComparisonOperator\\\":\\\"GreaterThanOrEqualToThreshold\\\",\\\"Threshold\\\":0.0}}\",\"Timestamp\":\"2015-05-18T21:29:02.335Z\",\"SignatureVersion\":\"1\",\"Signature\":\"NDW1h8yZFho++38J+iteAppMDodQn0G/ex1M03ns3F49wNiY1kIozmiHD7TSGkPZZ0GEOVbI1uEqp7hpqta2XcRgevJZELyb4N++G0HZDQcydmY5/qnAmYwWOx5Kyd7amWkOJJH/dZ5m/rNeS4CdoH8sUNU/E/K4Q17U79NCHVgJF+aTfAkYkcj7ElLKRG3gPgG8iR67HZ1dn23wNPPo1rZzYp3Cgy0TiYHtJrGVARStl8a5CZfppmy84hDQefR5I4iM4Xkwj+Xlrodv992cJ1BPWuciVo36h+tZoaSZ3QVU6FDETix+BClG9/wR+X1+fM5mnGKPuDu0OCl/9mqS2g==\",\"SigningCertUrl\":\"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-d6d679a1d18e95c2f9ffcf11f4f9e198.pem\",\"UnsubscribeUrl\":\"https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:074150922133:barclays-test-topic:1a1c3a30-271d-4a73-a840-97a26d3a0581\",\"MessageAttributes\":{}}}]}";

    runJiraComponentsTest();
  }

  public static String getSnsEventJsonContent() {
    return "{\"Records\":[{\"EventSource\":\"aws:sns\",\"EventVersion\":\"1.0\",\"EventSubscriptionArn\":\"arn:aws:sns:us-east-1:"+TEST_AWS_ACCOUNT_ID+":barclays-test-topic:1a1c3a30-271d-4a73-a840-97a26d3a0581\",\"Sns\":{\"Type\":\"Notification\",\"MessageId\":\"55e9f511-b020-57bd-be60-b8c5ebfe07d7\",\"TopicArn\":\"arn:aws:sns:us-east-1:"+TEST_AWS_ACCOUNT_ID+":barclays-test-topic\",\"Subject\":\"ALARM: \\\"paas-sps-s3-check-completed\\\" in US - N. Virginia\",\"Message\":\"{\\\"AlarmName\\\":\\\"paas sps s3 Check WorkflowsTimedOut\\\",\\\"AlarmDescription\\\":\\\"{\\\\\\\"project\\\\\\\":\\\\\\\"DPT\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"Platform\\\\\\\",\\\\\\\"priority\\\\\\\":\\\\\\\"3\\\\\\\",\\\\\\\"recipient\\\\\\\":\\\\\\\"test\\\\\\\"}\\\",\\\"AWSAccountId\\\":\\\""+TEST_AWS_ACCOUNT_ID+"\\\",\\\"NewStateValue\\\":\\\"ALARM\\\",\\\"NewStateReason\\\":\\\"Threshold Crossed: 1 datapoint (1.0) was greater than or equal to the threshold (0.0).\\\",\\\"StateChangeTime\\\":\\\"2015-05-18T21:29:02.273+0000\\\",\\\"Region\\\":\\\"US - N. Virginia\\\",\\\"OldStateValue\\\":\\\"OK\\\",\\\"Trigger\\\":{\\\"MetricName\\\":\\\"WorkflowsCompleted\\\",\\\"Namespace\\\":\\\"AWS/SWF\\\",\\\"Statistic\\\":\\\"SUM\\\",\\\"Unit\\\":null,\\\"Dimensions\\\":[{\\\"name\\\":\\\"WorkflowTypeVersion\\\",\\\"value\\\":\\\"0.0.1\\\"},{\\\"name\\\":\\\"Domain\\\",\\\"value\\\":\\\"paas-sps\\\"},{\\\"name\\\":\\\"WorkflowTypeName\\\",\\\"value\\\":\\\"S3Provisioner.check\\\"}],\\\"Period\\\":60,\\\"EvaluationPeriods\\\":1,\\\"ComparisonOperator\\\":\\\"GreaterThanOrEqualToThreshold\\\",\\\"Threshold\\\":0.0}}\",\"Timestamp\":\"2015-05-18T21:29:02.335Z\",\"SignatureVersion\":\"1\",\"Signature\":\"NDW1h8yZFho++38J+iteAppMDodQn0G/ex1M03ns3F49wNiY1kIozmiHD7TSGkPZZ0GEOVbI1uEqp7hpqta2XcRgevJZELyb4N++G0HZDQcydmY5/qnAmYwWOx5Kyd7amWkOJJH/dZ5m/rNeS4CdoH8sUNU/E/K4Q17U79NCHVgJF+aTfAkYkcj7ElLKRG3gPgG8iR67HZ1dn23wNPPo1rZzYp3Cgy0TiYHtJrGVARStl8a5CZfppmy84hDQefR5I4iM4Xkwj+Xlrodv992cJ1BPWuciVo36h+tZoaSZ3QVU6FDETix+BClG9/wR+X1+fM5mnGKPuDu0OCl/9mqS2g==\",\"SigningCertUrl\":\"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-d6d679a1d18e95c2f9ffcf11f4f9e198.pem\",\"UnsubscribeUrl\":\"https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:074150922133:barclays-test-topic:1a1c3a30-271d-4a73-a840-97a26d3a0581\",\"MessageAttributes\":{}}}]}";
  }
}
