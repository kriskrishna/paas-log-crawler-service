package org.familysearch.paas.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Help construct the configuration elements by using the Environment to get them.
 * This will allow 'testing' to inject the URLs etc. that are required to run locally,
 * but leave the 'production' values in place if there is no Env configuration present.
 *
 * @author roskelleycj
 */
public class ConfigurationHelper {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationHelper.class);


  private static final String PRODUCTION_JIRA_URL = "https://almtools.ldschurch.org:443";
  private static final String PROPERTY_TEST_JIRA_URL = "TEST_JIRA_URL";
  private static final String PROPERTY_TEST_JIRA_CREDENTIALS_FILE = "TEST_JIRA_CREDENTIALS_FILE";

  /**
   * Gives the JIRA Base URL that will be used in all JIRA HTTP requests.
   * To enable testing you simply define TEST_JIRA_URL as a System Property.
   *
   * @return a String representing the JIRA URL.
   */
  public String jiraBaseUrl() {
    return System.getProperty(PROPERTY_TEST_JIRA_URL, PRODUCTION_JIRA_URL);
  }

  /**
   * Gives an InputStream that contains the JIRA Credentials in a JSON document form.
   * This allows overriding reading the credentials from an S3 Bucket/Key for local
   * testing.
   * To enable testing you simply define TEST_JIRA_CREDENTIALS_FILE with the the full
   * path to a file.
   *
   * @return an InputStream that contains the JIRA Credentials in JSON document form.
   */
  public InputStream jiraCredentialsInputStream() {
    String fileName = System.getProperty(PROPERTY_TEST_JIRA_CREDENTIALS_FILE);
    InputStream inputStream = null;
    if (fileName != null) {
      try {
        File file = new File(fileName);
        inputStream = new FileInputStream(file);
      }
      catch (FileNotFoundException e) {
        LOG.error("Error occurred trying to read file={} for JIRA Credentials.  Will assume production defaults and continue.", fileName, e);
      }
    }
    return inputStream;
  }
}
