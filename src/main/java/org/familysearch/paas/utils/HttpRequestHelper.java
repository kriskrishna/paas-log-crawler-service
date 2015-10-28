package org.familysearch.paas.utils;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class HttpRequestHelper {

  private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHelper.class);

  /**
   * The proxy variables as defined in the System Properties.
   */
  public static final String HTTP_PROXYHOST_PROPERTY = "http.proxyHost";
  /**
   * The proxy variables as defined in the System Properties.
   */
  public static final String HTTP_PROXYPORT_PROPERTY = "http.proxyPort";
  private static final String COOKIE_SEPERATOR = ";";

  private final JsonFactory jsonFactory = new GsonFactory();
  private final String acceptHeader = Json.MEDIA_TYPE;
  private final String contentTypeHeader = Json.MEDIA_TYPE;

  private final Proxy proxy;
  private HttpTransport httpTransport;

  /**
   * Construct the helper.
   */
  public HttpRequestHelper() {
    // Explicitly configure the HTTP proxy if the standard http proxy system properties are set
    String proxyHost = System.getProperty(HTTP_PROXYHOST_PROPERTY);
    String proxyPort = System.getProperty(HTTP_PROXYPORT_PROPERTY);
    proxy = createProxy(proxyHost, proxyPort);
    this.httpTransport = new NetHttpTransport.Builder().setProxy(proxy).build();
  }

  /**
   * Executes an HTTP PUT operation against the supplied URL, body and Cookie header value.  Returns the resulting
   * HTTP status code as an int, throwing an IOException if the connection fails.  Note that the default google HttpClient
   * behavior to throw an exception in the event of a non-2XX status code is suppressed here.  The status code is returned
   * as is for the calling code to determine what to do with it.
   *
   * @param url        a string containing the URL that the PUT operation should be performed on
   * @param body       a bean representing the body of the request
   * @param cookies    the value of the Set-Cookie header that will be added to the request
   * @return the http status code resulting from the PUT request
   * @throws IOException
   */
  public int executePutRequest(String url, Object body, List<String> cookies) throws IOException {
    return executeRequestAndReturnStatusCode(url, body, cookies, HttpMethods.PUT);
  }

  /**
   * Executes an HTTP POST operation against the supplied URL, body and Cookie header value.  Returns the resulting
   * HTTP status code as an int, throwing an IOException if the connection fails.  Note that the default google HttpClient
   * behavior to throw an exception in the event of a non-2XX status code is suppressed here.  The status code is returned
   * as is for the calling code to determine what to do with it.
   *
   * @param url        a string containing the URL that the POST operation should be performed on
   * @param body       a bean representing the body of the request
   * @param cookies    the value of the Set-Cookie header that will be added to the request
   * @return the http status code resulting from the POST request
   * @throws IOException
   */
  public int executePostRequest(String url, Object body, List<String> cookies) throws IOException {
    return executeRequestAndReturnStatusCode(url, body, cookies, HttpMethods.POST);
  }

  /**
   * Executes an HTTP POST operation against the supplied URL, body and will determine the Cookies that should be
   * used for subsequent HTTP requests.
   *
   * @param url        a string containing the URL that the POST operation should be performed on
   * @param body       a bean representing the body of the request
   * @return is a List of Headers that should be used in HTTP requests setting the Cookie.
   * @throws IOException
   */
  public List<String> executePostLoginRequest(String url, Object body) throws IOException {
    HttpResponse httpResponse = executeRequest(HttpMethods.POST, url, body, null, true);

    List<String> fixedCookieHeader = new ArrayList<>();
    List<String> cookieHeaders = httpResponse.getHeaders().getHeaderStringValues("Set-Cookie");
    for (String cookieHeader : cookieHeaders) {
      cookieHeader += COOKIE_SEPERATOR; // make sure that there is at least one.  Removes conditional logic.
      String cookie = cookieHeader.substring(0, cookieHeader.indexOf(COOKIE_SEPERATOR));
      fixedCookieHeader.add(cookie);
    }
    return fixedCookieHeader;
  }
  /**
   * Executes an HTTP POST operation against the supplied URL, body and Cookie header value.  Returns the response
   * body as an object of type T, throwing an IOException if the connection fails or gives back a non-2XX status code, in
   * which case a @link com.google.api.client.http.HttpResponseException containing the status code is thrown.
   *
   * @param url        a string containing the URL that the POST operation should be performed on
   * @param body       a bean representing the body of the request
   * @param cookies    the value of the Set-Cookie header that will be added to the request
   * @param type       the type that the response body should be returned as
   * @return the http status code resulting from the POST request
   * @throws IOException
   */
  public <T> T executePostRequest(String url, Object body, List<String> cookies, Class<T> type) throws IOException {
    HttpResponse httpResponse = executeRequest(HttpMethods.POST, url, body, cookies, true);
    try {
      return httpResponse.parseAs(type);
    } finally {
      httpResponse.disconnect();
    }
  }

  private int executeRequestAndReturnStatusCode(String url, Object body, List<String> cookies, String method) throws IOException {
    HttpResponse httpResponse = executeRequest(method, url, body, cookies, false);
    try {
      if(!httpResponse.isSuccessStatusCode()) {
        // Since this method only returns the status code, the message will be lost unless it is retrieved and logged here
        String responseContent = new HttpResponseException.Builder(httpResponse).getContent();
        LOG.error(String.format("Failed HTTP %s request to url=\"%s\", body=%s, responseContent=%s", method, url, body, responseContent));
      }
      return httpResponse.getStatusCode();
    }
    finally {
      httpResponse.disconnect();
    }
  }

  /**
   * Allow raw access to the HttpResponse.  This is ideal for getting content and logging it.
   * @param method that will be used in the HTTP Request.
   * @param url that will be requested.
   * @param body that will be included in the request.
   * @param cookies that will be used to make the request.
   * @param throwExceptionOnNon2xxStatusCode used to control if errors are thrown for non 200 HTTP code values.
   * @return an HttpResponse.
   * @throws IOException if there is an error.
   */
  public HttpResponse executeRequest(String method,
                                      String url,
                                      Object body,
                                      List<String> cookies,
                                      boolean throwExceptionOnNon2xxStatusCode) throws IOException {

    HttpRequest request = httpTransport.createRequestFactory().buildRequest(
        method, new GenericUrl(url), new JsonHttpContent(jsonFactory, body));

    request.setParser(new JsonObjectParser(jsonFactory));

    // Set whether or not to throw an HttpResponseException for non-2XX http status codes
    request.setThrowExceptionOnExecuteError(throwExceptionOnNon2xxStatusCode);

    // Set the Accept and Content-Type headers
    HttpHeaders httpHeaders = new HttpHeaders().setAccept(acceptHeader).setContentType(contentTypeHeader);

    if (cookies != null && !cookies.isEmpty()) {
      for (String cookie : cookies) {
        String existingCookie;
        if (httpHeaders.getCookie() == null) {
          existingCookie = cookie;
        }
        else {
          existingCookie = httpHeaders.getCookie() + COOKIE_SEPERATOR + cookie;
        }
        httpHeaders.setCookie(existingCookie);
      }
    }

    request.setHeaders(httpHeaders);
    HttpResponse httpResponse = request.execute();

    LOG.debug(String.format("Executed HTTP request with url=\"%s\", method=%s, http_status_code=%s", url, method, httpResponse.getStatusCode()));

    return httpResponse;
  }

  public Proxy getProxy() {
    return proxy;
  }

  /**
   * Package protected method so that unit tests can run REALLY fast.  I.e. this method is REALLY slow.
   * @param proxyHost that will be used.
   * @param proxyPort that will be used.
   * @return a Proxy
   */
  Proxy createProxy(String proxyHost, String proxyPort) {
    if (proxyHost != null && proxyPort != null) {
      LOG.info("Using HTTP proxy '{}:{}'", proxyHost, proxyPort);
      return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.valueOf(proxyPort)));
    }
    else {
      LOG.info("No HTTP proxy has been configured by setting the '{}' and '{}' system properties",
          HTTP_PROXYHOST_PROPERTY, HTTP_PROXYPORT_PROPERTY);
      return Proxy.NO_PROXY;
    }
  }

  public void setHttpTransport(HttpTransport httpTransport) {
    this.httpTransport = httpTransport;
  }
}