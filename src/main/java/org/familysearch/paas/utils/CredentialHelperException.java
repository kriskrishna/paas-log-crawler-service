package org.familysearch.paas.utils;

/**
 * Exception that is thrown when errors occur using CredentialHelper class.
 *
 * @author barclays
 */
public class CredentialHelperException extends RuntimeException {

  /**
   * Contruct an exception message.
   *
   * @param message to be added.
   */
  public CredentialHelperException(String message) {
    super(message);
  }

  /**
   * Construct an exception with a message and a cause.
   *
   * @param message to be used
   * @param cause parent exception
   */
  public CredentialHelperException(String message, Throwable cause) {
    super(message, cause);
  }
}
