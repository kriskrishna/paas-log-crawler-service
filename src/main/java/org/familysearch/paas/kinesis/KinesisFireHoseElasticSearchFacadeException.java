package org.familysearch.paas.kinesis;

/**
 * JIRA Facade exceptions.
 *
 * @author roskelleycj
 */
public class KinesisFireHoseElasticSearchFacadeException extends RuntimeException {
  /**
   * Constructor to wrap specific JIRA exceptions.
   * @param message that describes the conditions at the time the exception occurred.
   * @param cause that will be wrapped.
   */
  public KinesisFireHoseElasticSearchFacadeException(String message, Throwable cause) {
    super(message, cause);
  }
}
