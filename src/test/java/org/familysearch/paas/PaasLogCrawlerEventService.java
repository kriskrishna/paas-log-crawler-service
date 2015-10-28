package org.familysearch.paas;

import com.google.api.client.util.Charsets;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import org.familysearch.paas.kinesis.JiraFacade;
import org.familysearch.paas.kinesis.KinesisFireHoseElasticSearchFacadeException;
import org.familysearch.paas.utils.CredentialHelper;
import org.familysearch.paas.utils.CredentialHelperTest;
import org.familysearch.paas.utils.ParserHelper;
import org.familysearch.paas.utils.ParserHelperTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class PaasLogCrawlerEventService {

  private static final String USERNAME = "username";
  private static final int CONFIG_ERROR_CODE = -1;
  private static final int UNKNOWN_EXCEPTION_ERROR_CODE = -3;
  @Mock
  private CredentialHelper mockCredentialHelper;
  @Mock
  private ParserHelper mockParseHelper;
  
  @Captor
  private ArgumentCaptor<Map<String, String>> inputCaptor;

  private PaasLogCrawlerEventService testModel;
  private java.util.Map<java.lang.String, java.lang.String> credentialMap;
  private String testEvent;

  @BeforeMethod
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    testModel = new PaasLogCrawlerEventService(mockCredentialHelper, mockParseHelper) {
      @Override
      InputStream getFileInputStream(String eventPath) throws IOException {
        return getInputStream(testEvent);
      }

      private InputStream getInputStream(String input) throws IOException {
        byte[] bytes = input.getBytes(Charsets.UTF_8);
        return new ByteArrayInputStream(bytes);
      }

    };

    setupSnsEvent();
    setupCredentialMap();
  }

  private void setupCredentialMap() {
    // We could probably extract this and put it into the CredentialHelperTest
    credentialMap = new HashMap<>();
    credentialMap.put(USERNAME, TEST_USERNAME);
    // etc.  We really do not need all of them.  Just enough to detect that
    // the jiraMap and the credentialMap have been merged together.

    String credentialJsonContents = CredentialHelperTest.getCredentialJsonContents();
    when(mockCredentialHelper.getJiraCreds(TEST_AWS_ACCOUNT_ID)).thenReturn(credentialJsonContents);
    when(mockParseHelper.getJiraCreds(credentialJsonContents)).thenReturn(credentialMap);
  }

  private void setupSnsEvent() {
    testEvent = ParserHelperTest.getSnsEventJsonContent();
    when(mockParseHelper.getKinesisComponents(testEvent)).thenCallRealMethod();
  }

  private int runTestProcessEvent() {
    return testModel.processEvent("some valid path");
  }

  @Test
  public void itShouldMergeCredentialMap() {

    runTestProcessEvent();

    verify(mockJiraFacade).createNotification(inputCaptor.capture());
    assertThatInputContainsUsername();
  }

  
}