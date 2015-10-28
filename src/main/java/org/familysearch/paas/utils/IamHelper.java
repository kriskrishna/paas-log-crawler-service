package org.familysearch.paas.utils;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that performs IAM functions.
 */
public class IamHelper {
  private static final Logger LOG = LoggerFactory.getLogger(IamHelper.class);

  /**
   * Unknown Account Alias.
   */
  public static final String UNKNOWN = "unknown";
  private AmazonIdentityManagementClient iamClient;

  /**
   * Default Constructor.
   */
  public IamHelper() {
    this(new AmazonIdentityManagementClient());
  }

  /**
   * Constructor for testing.
   * @param iamClient that should be used.  Generally a mock.
   */
  public IamHelper(AmazonIdentityManagementClient iamClient) {
    this.iamClient = iamClient;
  }

  /**
   * Will get first alias associated with the account.
   * @return a String representing the first alias associated with the account.
   */
  public String getFirstAccountAlias() {
    try {
      return iamClient.listAccountAliases().getAccountAliases().get(0);
    }
    catch (Exception e) {
      LOG.error("Error occurred looking for Account Alias.  Will assume {} for now.  Perhaps the account alias has not been configured yet?", UNKNOWN, e);
      return UNKNOWN;
    }
  }
}
