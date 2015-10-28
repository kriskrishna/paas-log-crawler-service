package org.familysearch.paas.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.api.client.util.Charsets;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * Unit Tests for CredentialHelper class.
 *
 * @author barclays
 */
public class CredentialHelperTest {

  private static final String TEST_AWS_ACCOUNT_ID = "000111222333";
  private static final String TEST_BUCKET_NAME = "account-info-"+ TEST_AWS_ACCOUNT_ID;
  private static final String TEST_KEY_NAME = "superadmin/us-east-1/jira-creds";

  @Mock
  private AmazonS3Client mockS3Client;
  @Mock
  private S3Object mockS3Object;
  @Mock
  private S3ObjectInputStream mockS3ObjectInputStream;
  @Mock
  private ConfigurationHelper mockConfigurationHelper;

  private CredentialHelper testModel;

  @BeforeMethod
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    doReturn(mockS3Object).when(mockS3Client).getObject(eq(TEST_BUCKET_NAME), eq(TEST_KEY_NAME));
    mockS3ObjectInputStream = new S3ObjectInputStream(getInputStream(getCredentialJsonContents()), null);
    when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
    when(mockConfigurationHelper.jiraCredentialsInputStream()).thenReturn(null);
    testModel = new CredentialHelper(mockS3Client, mockConfigurationHelper);
  }

  public static String getCredentialJsonContents() {
    return getCredentialJsonContents("myUsername", "myPass");
  }

  public static String getCredentialJsonContents(String username, String password) {
    return String.format("{\"username\" : \"%s\", \"password\" : \"%s\"}", username, password);
  }

  private InputStream getInputStream(String input) throws IOException {
    byte[] bytes = input.getBytes(Charsets.UTF_8);
    return new ByteArrayInputStream(bytes);
  }

  @Test
  public void itShouldReturnAString() {

    assertEquals(runTestJiraCreds(), getCredentialJsonContents());
  }

  private String runTestJiraCreds() {
    return testModel.getJiraCreds(TEST_AWS_ACCOUNT_ID);
  }

  @Test (expectedExceptions = CredentialHelperException.class)
  public void itShouldErrorWhenCantGetObjectContent() throws AmazonServiceException{
    when(mockS3Client.getObject(anyString(), anyString())).thenThrow(new AmazonServiceException("test message"));


    runTestJiraCreds();
  }

  @Test
  public void itShouldUseConfigurationHelper() {
    runTestJiraCreds();


    verify(mockConfigurationHelper).jiraCredentialsInputStream();
  }

  @Test
  public void itShouldUseConfigurationHelperInputStream() throws Exception {
    String credentialJsonContents = getCredentialJsonContents("testing1", "1234pass");
    when(mockConfigurationHelper.jiraCredentialsInputStream()).thenReturn(getInputStream(credentialJsonContents));

    assertEquals(runTestJiraCreds(), credentialJsonContents);
  }
}
