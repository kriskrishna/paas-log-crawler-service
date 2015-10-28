package org.familysearch.paas.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Makes calls to an S3 bucket that contain JIRA credentials.
 *
 * @author barclays
 */
public class CredentialHelper {
  private static final Logger LOG = LoggerFactory.getLogger(CredentialHelper.class);

  // If you expected another region, other then us-east-1 you will have to code it!
  // The %s is the AWS account ID
  private static final String BUCKET_NAME_PATTERN = "account-info-%s";
  private static final String KEY_NAME = "superadmin/us-east-1/jira-creds";

  private AmazonS3Client s3Client;
  private ConfigurationHelper configurationHelper;

  /**
   * Default Constructor.
   */
  public CredentialHelper(){
    this(new AmazonS3Client(), new ConfigurationHelper());
  }

  /**
   * Constructor for testing.
   *
   * @param s3Client Mock data source object
   */
  public CredentialHelper(AmazonS3Client s3Client, ConfigurationHelper configurationHelper){
    this.s3Client = s3Client;
    this.configurationHelper = configurationHelper;
  }

  /**
   * Get JIRA Creds out of S3 bucket.
   *
   * @return content of S3 object
   */
  public String getJiraCreds(String awsAccountId){
    InputStream objStream = configurationHelper.jiraCredentialsInputStream();
    String bucketName = String.format(BUCKET_NAME_PATTERN, awsAccountId);
    if (objStream == null) {
      S3Object content;
      try {
        content = this.s3Client.getObject(bucketName, KEY_NAME);
      }
      catch (AmazonServiceException e) {
        throw new CredentialHelperException("No Such Key. Verify that Bucket: \"" + bucketName + "\" and Key: \"" + KEY_NAME + "\" are in S3", e);
      }
      objStream = content.getObjectContent();
    }
    else {
      LOG.info("Did NOT read from bucket={} key={} but used testing configuration.", bucketName, KEY_NAME);
    }

    String streamOutput;
    try {
      streamOutput = IOUtils.toString(objStream);
    }
    catch (IOException e) {
      throw new CredentialHelperException("Failed to read Input Stream for JIRA Credentials!", e);
    }
    return streamOutput;
  }


}
