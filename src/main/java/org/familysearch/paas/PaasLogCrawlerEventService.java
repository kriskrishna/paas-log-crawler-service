package org.familysearch.paas;

import static net.logstash.logback.marker.Markers.append;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import net.logstash.logback.marker.ObjectAppendingMarker;

import org.familysearch.paas.kinesis.KinesisFireHoseElasticSearchFacadeException;
import org.familysearch.paas.utils.CredentialHelper;
import org.familysearch.paas.utils.ParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.util.IOUtils;

/**
 * A simple application that will create KinesisFireHose and may be S3.
 *
 * Simple means that you already know the SNS event message format etc.
 *
 * @author kriskrishna
 */
public class PaasLogCrawlerEventService {
  private static final Logger LOG = LoggerFactory.getLogger(PaasLogCrawlerEventService.class);
  private static final int SUCCESS = 0;
  private static final int CONFIG_ERROR = -1;
  private static final int UNKNOWN_ERROR = -3;

  private final CredentialHelper credentialHelper;
  private final ParserHelper parserHelper;

  /**
   * Program main.  Currently has no argument checking.
   * @param args should contain the SNS Event string.
   */
  public static void main(String[] args) {
    System.exit(init(args));
  }

  /**
   * Initialize the process execution with arguments.  Attempt to separate testing of main without exiting.
   * @param args that are used as input.
   * @return exit code, where 0 means success and any other value is an error code.
   */
  public static int init(String[] args) {
    PaasLogCrawlerEventService paasLogCrawlerEventService = new PaasLogCrawlerEventService();

    reportVersion();

    return paasLogCrawlerEventService.processEvent(args[0]);
  }

  private static void reportVersion() {
    InputStream inputStream = null;
    try {
      inputStream = PaasLogCrawlerEventService.class.getClassLoader().getResourceAsStream("cm.properties");
      Properties props = new Properties();
      props.load(inputStream);
      String version = (String) props.get("build.version");
      System.out.println(String.format("VERSION: %s", version));
    }
    catch (IOException e) {
      LOG.warn("Error trying to load the version!  Does cm.properties exist on the classpath?", e);
    }
    finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        }
        catch (IOException e) {
          LOG.info("Error trying to close cm.properties input stream.  Will ignore.", e);
        }
      }
    }
  }

  /**
   * Default Constructor.
   */
  public PaasLogCrawlerEventService() {
    this(new CredentialHelper(), new ParserHelper());
  }

  /**
   * Construct with specific instances.
   * @param credentialHelper that will be used.
   * @param parserHelper that will be used.
   * @param jiraFacade that will be used.
   */
  public PaasLogCrawlerEventService(CredentialHelper credentialHelper, ParserHelper parserHelper) {
    this.credentialHelper = credentialHelper;
    this.parserHelper = parserHelper;
  }

  /**
   * The main entry point.
   *
   * @param eventPath that will be processed.
   */
  public int processEvent(String eventPath) {
    Map<String, String> map = null;
    int processStatus = 0;
    ObjectAppendingMarker marker = append("eventPath", eventPath);
    try {
      map = getKinesisFireHoseElasticSearchClusterInputs(loadEvent(eventPath));
    }
    catch (Exception e) {
      LOG.error(marker, "Error occurred trying to get configuration.", e);
      processStatus = CONFIG_ERROR;
    }

    if (processStatus == SUCCESS) {
      try {
      }
      catch (KinesisFireHoseElasticSearchFacadeException e) {
      }
      catch (Exception e) {
        LOG.error(marker, "Error occurred trying to create notification.", e);
        processStatus = UNKNOWN_ERROR;
      }
    }
    return processStatus;
  }

  private String getAwsAccountId(Map<String, String> map) {
    return map.get(ParserHelper.AWS_ACCOUNT_ID);
  }

  private String loadEvent(String eventPath) {
    try {
      InputStream inputStream = getFileInputStream(eventPath);
      return IOUtils.toString(inputStream);
    }
    catch (IOException e) {
      throw new RuntimeException("Error occurred reading file=" + eventPath, e);
    }
  }

  // Allow the test to override this.
  InputStream getFileInputStream(String eventPath) throws IOException {
    return new FileInputStream(eventPath);
  }

  private Map<String, String> getKinesisFireHoseElasticSearchClusterInputs(String event) {
    return parserHelper.getKinesisComponents(event);
  }
}
