package org.familysearch.paas.utils;

/**
 * Exception that is thrown when errors occur using ParserHelper class.
 *
 * @author barclays
 */
public class ParserHelperException extends RuntimeException {
  /**
   * Contruct an exception message.
   *
   * @param message to be added.
   */
  public ParserHelperException(String message) {
    super(message);
  }

  /**
   * Construct an exception with a message and a cause.
   *
   * @param message to be used
   * @param cause parent exception
   */
  public ParserHelperException(String message, Throwable cause) {
    super(message, cause);
  }
}
