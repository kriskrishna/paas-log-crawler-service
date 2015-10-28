package org.familysearch.paas.utils;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;

import static org.testng.Assert.*;

public class ConfigurationHelperTest {

  private static final String TEST_JIRA_URL_VALUE = "http://testjira.org";
  private static final String SYS_PROPERTY_TEST_JIRA_URL = "TEST_JIRA_URL";
  private static final String SYS_PROPERTY_TEST_JIRA_CREDENTIALS_FILE = "TEST_JIRA_CREDENTIALS_FILE";

  @Mock
  private InputStream mockInputStream;

  private ConfigurationHelper testModel;

  @BeforeMethod
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    testModel = new ConfigurationHelper();
  }

  @Test
  public void jiraBase_itShouldReturnProductionJiraUrlWhenNoEnvDefined() {
    assertEquals("https://almtools.ldschurch.org:443", testModel.jiraBaseUrl());
  }

  @Test
  public void jiraBase_itShouldReturnEnvDefinedValue() {
    System.setProperty(SYS_PROPERTY_TEST_JIRA_URL, TEST_JIRA_URL_VALUE);
    try {
      assertEquals(testModel.jiraBaseUrl(), TEST_JIRA_URL_VALUE);
    }
    finally {
     // Make sure we clean up the mess, so no other tests will be affected.
     System.clearProperty(SYS_PROPERTY_TEST_JIRA_URL);
    }
  }

  @Test
  public void jiraCredentialsInputStream_itShouldReturnNullWhenNoEnvDefined() {
    assertNull(testModel.jiraCredentialsInputStream());
  }

  @Test
  public void jiraCredentialsInputStream_itShouldReturnEnvDefinedStream() throws Exception {
    File tempFile = File.createTempFile("jiraCredentialsInputStream", "json");
    tempFile.deleteOnExit();
    System.setProperty(SYS_PROPERTY_TEST_JIRA_CREDENTIALS_FILE, tempFile.getAbsolutePath());
    try {
      InputStream inputStream = testModel.jiraCredentialsInputStream();
      assertNotNull(inputStream);
      inputStream.close();
    }
    finally {
      // Make sure we clean up the mess, so no other tests will be affected.
      System.clearProperty(SYS_PROPERTY_TEST_JIRA_CREDENTIALS_FILE);
    }
  }

  @Test
  public void jiraCredentialsInputStream_itShouldReturnNullWhenFileDoesnotExsit() {
    System.setProperty(SYS_PROPERTY_TEST_JIRA_CREDENTIALS_FILE, "/tmp/somerandomfile");
    try {
      assertNull(testModel.jiraCredentialsInputStream());
    }
    finally {
      // Make sure we clean up the mess, so no other tests will be affected.
      System.clearProperty(SYS_PROPERTY_TEST_JIRA_CREDENTIALS_FILE);
    }
  }
}