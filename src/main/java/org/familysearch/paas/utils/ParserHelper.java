package org.familysearch.paas.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses both Jira Credentials and SNS Events
 *
 * @author barclays
 */
public class ParserHelper {

  private static final Logger LOG = LoggerFactory.getLogger(ParserHelper.class);

  private static final String ALARM_DESCRIPTION = "AlarmDescription";
  /**
   * The Key for the AWS Account Id in the jira components map.
   */
  public static final String AWS_ACCOUNT_ID = "aws_account_id";
  private static final String DESCRIPTION = "Reason:  %s \nAlarm Link: %s\nRunbook Link: %s";
  // Hard coded to us-east-1 as we do not yet have the CloudWatch Event Service client in any other region.
  private static final String ALARM_URL_PATTERN = "https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#alarm:alarmFilter=ANY;name=%s";
  private static final String RUNBOOK_URL_PATTERN = "https://almtools.ldschurch.org/fhconfluence/dosearchsite.action?where=ORB&spaceSearch=true&queryString=%s";

  private IamHelper iamHelper;

  /**
   * Default Constructor.
   */
  public ParserHelper() {
    this(new IamHelper());
  }

  /**
   * Constructor with specific instances of memebers.
   * @param iamHelper that will be used.
   */
  public ParserHelper(IamHelper iamHelper) {
    this.iamHelper = iamHelper;
  }

  /**
   * Gathers the JIRA Credentials and puts them in a map that will be used when sending requests to JIRA.
   *
   * @param streamOutput retrived from S3 bucket.
   * @return jiraCreds a map of credentials for JIRA.
   */
  public Map<String, String> getJiraCreds(String streamOutput){
    Map<String, String> jiraCreds = new HashMap<>();


    JsonObject s3content = new JsonParser().parse(streamOutput).getAsJsonObject();

    jiraCreds.put("username", s3content.get("username").getAsString());
    jiraCreds.put("password", s3content.get("password").getAsString());

    return jiraCreds;
  }

  public Map<String, String> getKinesisComponents(String snsEvent){
    Map<String, String> jiraComponents = new HashMap<>();
    JsonParser jsonParser = new JsonParser();
    JsonObject snsSection;
    try {
      snsSection = (JsonObject) jsonParser.parse(snsEvent).getAsJsonObject().getAsJsonArray("Records").get(0).getAsJsonObject().get("Sns");
    }
    catch(NullPointerException e) {
      throw new ParserHelperException("JSON document does not appear to be valid!", e);
    }

    String message = snsSection.get("Message").getAsString();
    JsonObject messageSection;
    messageSection = jsonParser.parse(message).getAsJsonObject();
    String alarmDescription = messageSection.get(ALARM_DESCRIPTION).getAsString();
    JsonObject alarmDesJson;
    try {
      alarmDesJson = (JsonObject) jsonParser.parse(alarmDescription);
    }
    catch (JsonSyntaxException e) {
      throw new ParserHelperException("Alarm Description is not valid!", e);
    }

    String alarmName = messageSection.getAsJsonObject().get("AlarmName").getAsString();
    String region = messageSection.getAsJsonObject().get("Region").getAsString();
    String summary = "CloudWatch Alert [ " + getAccountFriendlyName() + " - " + region + " ] - " + alarmName;
    String summarySearch = replaceSpecialCharacters(summary);
    String description = String.format(DESCRIPTION, messageSection.getAsJsonObject().get("NewStateReason").getAsString(), getAlarmUrl(alarmName), getRunbookUrl(summarySearch));

    String accountId = messageSection.getAsJsonObject().get("AWSAccountId").getAsString();

    String project = alarmDesJson.getAsJsonObject().get("project").getAsString();
    String component = alarmDesJson.getAsJsonObject().get("component").getAsString();
    String priority = alarmDesJson.getAsJsonObject().get("priority").getAsString();
    String recipient = alarmDesJson.getAsJsonObject().get("recipient").getAsString();

    jiraComponents.put("summary", summary);
    jiraComponents.put("summary_search", summarySearch);
    jiraComponents.put("project", project);
    jiraComponents.put("component", component);
    jiraComponents.put("priority", priority);
    jiraComponents.put("recipient", recipient);
    jiraComponents.put("description", description);
    jiraComponents.put(AWS_ACCOUNT_ID, accountId);

    return jiraComponents;
  }

  private String getAccountFriendlyName() {
    if (iamHelper == null) {
      return IamHelper.UNKNOWN;
    }
    return replaceSpecialCharacters(iamHelper.getFirstAccountAlias());
  }

  private String getRunbookUrl(String summarySearch) {
    return convertToURL(RUNBOOK_URL_PATTERN, summarySearch);
  }

  private String getAlarmUrl(String alarmName) {
    return convertToURL(ALARM_URL_PATTERN, alarmName);
  }

  private String convertToURL(String urlPattern, String value) {
    try {
      // This is a PAIN!!!  Doesn't appear to be a simple library that will get this right.
      // I.e. without the + replaced as %20 AWS CloudWatch does not work correctly.
      value = URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
    }
    catch (UnsupportedEncodingException e) {
      LOG.info("Error encoding value={} and will ignore the error!", value, e);
    }
    return String.format(urlPattern, value);
  }

  private String replaceSpecialCharacters(String summary) {
    return summary.replaceAll("[+&|!(){}^~*?\\:\\[\\]-]", " ");
  }

}
