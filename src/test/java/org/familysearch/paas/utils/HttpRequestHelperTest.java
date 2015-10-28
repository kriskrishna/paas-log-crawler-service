package org.familysearch.paas.utils;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.client.util.Key;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Test
public class HttpRequestHelperTest
{

  private HttpRequestHelper fixture;
  private static final String TEST_URL = "http://localhost";
  private static final String TEST_COOKIE = "cookie 12345678987654321";
  private static final String TEST_PROXY_HOST = "proxy.fslocal.net";
  private static final String TEST_PROXY_PORT = "443";

  @Mock
  private Proxy mockProxy;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    fixture = new HttpRequestHelper() {
      @Override
      Proxy createProxy(String proxyHost, String proxyPort) {
        return mockProxy;
      }
    };
  }

  private MockLowLevelHttpResponse successfulResponse() {
    final MockLowLevelHttpResponse mockHttpResponse = new MockLowLevelHttpResponse();
    mockHttpResponse.setStatusCode(200);
    mockHttpResponse.setContentType(Json.MEDIA_TYPE);
    mockHttpResponse.setContent(getTestObjectAsJson());
    return mockHttpResponse;
  }

  private MockLowLevelHttpRequest createMockHttpRequestWithSuccessfulResponse() {
    return createMockHttpRequestWithSuccessfulResponse(successfulResponse());
  }

  private MockLowLevelHttpRequest createMockHttpRequestWithSuccessfulResponse(final MockLowLevelHttpResponse response) {
    return new MockLowLevelHttpRequest() {
      @Override
      public LowLevelHttpResponse execute() throws IOException {
        return response;
      }
    };
  }

  private MockLowLevelHttpRequest createMockHttpRequestResponseWithStatusCode(final int statusCode) {
    return new MockLowLevelHttpRequest() {
      @Override
      public LowLevelHttpResponse execute() throws IOException {
        final MockLowLevelHttpResponse mockHttpResponse = new MockLowLevelHttpResponse();
        mockHttpResponse.setStatusCode(statusCode);
        if (statusCode >= 200 && statusCode < 300) {
          // Mock out a successful response
          mockHttpResponse.setContentType(Json.MEDIA_TYPE);
          mockHttpResponse.setContent(getTestObjectAsJson());
        }
        return mockHttpResponse;
      }
    };
  }

  private MockLowLevelHttpRequest createMockHttpRequestThatAssertsHeader(final MockLowLevelHttpResponse response, final String header, final String headerValue) {
    return new MockLowLevelHttpRequest() {
      @Override
      public LowLevelHttpResponse execute() throws IOException {
        assertEquals(getFirstHeaderValue(header), headerValue);
        return response;
      }
    };
  }

  private MockLowLevelHttpRequest createMockHttpRequestThatAssertsContent(final MockLowLevelHttpResponse response, final String expectedJsonContent) {
    return new MockLowLevelHttpRequest() {
      @Override
      public LowLevelHttpResponse execute() throws IOException {
        assertEquals(getStreamingContent().getClass(), JsonHttpContent.class);
        assertEquals(getContentAsString(), expectedJsonContent);
        return response;
      }
    };
  }

  private void whenHttpRequest_ReturnHttpResponse(final MockLowLevelHttpRequest mockHttpRequestResponse) {
    MockHttpTransport mockHttpTransport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return mockHttpRequestResponse;
      }
    };
    fixture.setHttpTransport(mockHttpTransport);
  }

  //################# Constructor ######################################################################################
  @Test
  public void constructor_shouldExplicitlySetProxy_whenBothProxySystemPropertiesAreSet() throws Exception {
    final Map<String, String> trace = new HashMap<>();
    System.setProperty(HttpRequestHelper.HTTP_PROXYHOST_PROPERTY, TEST_PROXY_HOST);
    System.setProperty(HttpRequestHelper.HTTP_PROXYPORT_PROPERTY, TEST_PROXY_PORT);
    fixture = new HttpRequestHelper() {
      @Override
      Proxy createProxy(String proxyHost, String proxyPort) {
        trace.put("proxyHost", proxyHost);
        trace.put("proxyPort", proxyPort);
        return mockProxy;
      }
    };
    assertEquals(fixture.getProxy(), mockProxy);
    assertEquals(trace.get("proxyHost"), TEST_PROXY_HOST);
    assertEquals(trace.get("proxyPort"), TEST_PROXY_PORT);
  }

  @Test
  public void constructor_shouldNotUseAProxy_whenOnlyTheProxyHostPropertyIsSet() throws Exception {
    System.setProperty(HttpRequestHelper.HTTP_PROXYHOST_PROPERTY, TEST_PROXY_HOST);
    System.clearProperty(HttpRequestHelper.HTTP_PROXYPORT_PROPERTY);
    fixture = new HttpRequestHelper();
    assertEquals(fixture.getProxy(), Proxy.NO_PROXY);
  }

  @Test
  public void constructor_shouldNotUseAProxy_whenOnlyTheProxyPortPropertyIsSet() throws Exception {
    System.clearProperty(HttpRequestHelper.HTTP_PROXYHOST_PROPERTY);
    System.setProperty(HttpRequestHelper.HTTP_PROXYPORT_PROPERTY, TEST_PROXY_PORT);
    fixture = new HttpRequestHelper();
    assertEquals(fixture.getProxy(), Proxy.NO_PROXY);
  }

  @Test
  public void constructor_shouldNotUseAProxy_whenTheProxyPropertiesAreNotSet() throws Exception {
    System.clearProperty(HttpRequestHelper.HTTP_PROXYHOST_PROPERTY);
    System.clearProperty(HttpRequestHelper.HTTP_PROXYPORT_PROPERTY);
    fixture = new HttpRequestHelper();
    assertEquals(fixture.getProxy(), Proxy.NO_PROXY);
  }

  //################# POST with object return type #####################################################################

  @Test
  public void executePostRequest_shouldReturnTheResponsePayload_asAnObjectOfTheRequestedType() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestWithSuccessfulResponse());
    TestObject responseObject = fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
    assertEquals(getTestObject(), responseObject);
  }

  @Test
  public void executePostRequest_shouldSetCookieHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.COOKIE, TEST_COOKIE));
    fixture.executePostRequest(TEST_URL, getTestObject(), Arrays.asList(TEST_COOKIE), TestObject.class);
  }

  @Test
  public void executePostRequest_shouldSetCookieHeaderWithAllCookiesOfTheRequest() throws Exception {
    String otherCookie = "other=other cookie";
    String expectedCookieHeader = String.format("%s;%s", TEST_COOKIE, otherCookie);
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.COOKIE, expectedCookieHeader));
    fixture.executePostRequest(TEST_URL, getTestObject(), Arrays.asList(TEST_COOKIE, otherCookie), TestObject.class);
  }

  @Test
  public void executePostRequest_shouldSetAcceptHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.ACCEPT, Json.MEDIA_TYPE));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test
  public void executePostRequest_shouldSetContentTypeHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.CONTENT_TYPE, Json.MEDIA_TYPE));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test
  public void executePostRequest_shouldMarshallThePayloadObjectToJson() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsContent(successfulResponse(), getTestObjectAsJson()));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test
  public void executePostRequest_shouldSetUrlOfTheRequest() throws Exception {
    MockHttpTransport mockHttpTransportThatAssertsUrl = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        assertEquals(url, TEST_URL);
        return createMockHttpRequestWithSuccessfulResponse();
      }
    };
    fixture.setHttpTransport(mockHttpTransportThatAssertsUrl);
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test
  public void executePostRequest_shouldSetHttpMethodOfTheRequest() throws Exception {
    MockHttpTransport mockHttpTransportThatAssertsUrl = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        assertEquals(method, HttpMethods.POST);
        return createMockHttpRequestWithSuccessfulResponse();
      }
    };
    fixture.setHttpTransport(mockHttpTransportThatAssertsUrl);
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test(expectedExceptions = HttpResponseException.class)
  public void executePostRequest_shouldThrowAnException_whenTheResponseStatusCodeIs500() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(500));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test(expectedExceptions = HttpResponseException.class)
  public void executePostRequest_shouldThrowAnException_whenTheResponseStatusCodeIs404() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(404));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test(expectedExceptions = HttpResponseException.class)
  public void executePostRequest_shouldThrowAnException_whenTheResponseStatusCodeIs400() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(400));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test(expectedExceptions = HttpResponseException.class)
  public void executePostRequest_shouldThrowAnException_whenTheResponseStatusCodeIs301() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(301));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  @Test
  public void executePostRequest_shouldNotThrowAnException_whenTheResponseStatusCodeIs201() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(201));
    fixture.executePostRequest(TEST_URL, getTestObject(), null, TestObject.class);
  }

  //################# POST with status int return type #################################################################

  @Test
  public void executePostRequestWithStatusCode_shouldReturnTheStatusCode_whenTheResponseIsSuccessful() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestWithSuccessfulResponse());
    int responseStatusCode = fixture.executePostRequest(TEST_URL, getTestObject(), null);
    assertEquals(responseStatusCode, 200);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldReturnTheStatusCode_whenTheResponseStatusCodeIs500() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(500));
    int responseStatusCode = fixture.executePostRequest(TEST_URL, getTestObject(), null);
    assertEquals(responseStatusCode, 500);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldReturnTheStatusCode_whenTheResponseStatusCodeIs400() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(400));
    int responseStatusCode = fixture.executePostRequest(TEST_URL, getTestObject(), null);
    assertEquals(responseStatusCode, 400);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldSetCookieHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.COOKIE, TEST_COOKIE));
    fixture.executePostRequest(TEST_URL, getTestObject(), Arrays.asList(TEST_COOKIE));
  }

  @Test
  public void executePostRequestWithStatusCode_shouldSetAcceptHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.ACCEPT, Json.MEDIA_TYPE));
    fixture.executePostRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldSetContentTypeHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.CONTENT_TYPE, Json.MEDIA_TYPE));
    fixture.executePostRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldMarshallThePayloadObjectToJson() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsContent(successfulResponse(), getTestObjectAsJson()));
    fixture.executePostRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldSetUrlOfTheRequest() throws Exception {
    MockHttpTransport mockHttpTransportThatAssertsUrl = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        assertEquals(url, TEST_URL);
        return createMockHttpRequestWithSuccessfulResponse();
      }
    };
    fixture.setHttpTransport(mockHttpTransportThatAssertsUrl);
    fixture.executePostRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePostRequestWithStatusCode_shouldSetHttpMethodOfTheRequest() throws Exception {
    MockHttpTransport mockHttpTransportThatAssertsUrl = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        assertEquals(method, HttpMethods.POST);
        return createMockHttpRequestWithSuccessfulResponse();
      }
    };
    fixture.setHttpTransport(mockHttpTransportThatAssertsUrl);
    fixture.executePostRequest(TEST_URL, getTestObject(), null);
  }

  //################# PUT ##############################################################################################

  @Test
  public void executePutRequest_shouldReturnTheStatusCode_whenTheResponseIsSuccessful() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestWithSuccessfulResponse());
    int responseStatusCode = fixture.executePutRequest(TEST_URL, getTestObject(), null);
    assertEquals(responseStatusCode, 200);
  }

  @Test
  public void executePutRequest_shouldThrowAnException_whenTheResponseStatusCodeIs500() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(500));
    int responseStatusCode = fixture.executePutRequest(TEST_URL, getTestObject(), null);
    assertEquals(responseStatusCode, 500);
  }

  @Test
  public void executePutRequest_shouldThrowAnException_whenTheResponseStatusCodeIs400() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestResponseWithStatusCode(400));
    int responseStatusCode = fixture.executePutRequest(TEST_URL, getTestObject(), null);
    assertEquals(responseStatusCode, 400);
  }

  @Test
  public void executePutRequest_shouldSetCookieOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.COOKIE, TEST_COOKIE));
    fixture.executePutRequest(TEST_URL, getTestObject(), Arrays.asList(TEST_COOKIE));
  }

  @Test
  public void executePutRequest_shouldSetAcceptHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.ACCEPT, Json.MEDIA_TYPE));
    fixture.executePutRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePutRequest_shouldSetContentTypeHeaderOfTheRequest() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsHeader(successfulResponse(), HttpHeaders.CONTENT_TYPE, Json.MEDIA_TYPE));
    fixture.executePutRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePutRequest_shouldMarshallThePayloadObjectToJson() throws Exception {
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestThatAssertsContent(successfulResponse(), getTestObjectAsJson()));
    fixture.executePutRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePutRequest_shouldSetUrlOfTheRequest() throws Exception {
    MockHttpTransport mockHttpTransportThatAssertsUrl = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        assertEquals(url, TEST_URL);
        return createMockHttpRequestWithSuccessfulResponse();
      }
    };
    fixture.setHttpTransport(mockHttpTransportThatAssertsUrl);
    fixture.executePutRequest(TEST_URL, getTestObject(), null);
  }

  @Test
  public void executePutRequest_shouldSetHttpMethodOfTheRequest() throws Exception {
    MockHttpTransport mockHttpTransportThatAssertsUrl = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        assertEquals(method, HttpMethods.PUT);
        return createMockHttpRequestWithSuccessfulResponse();
      }
    };
    fixture.setHttpTransport(mockHttpTransportThatAssertsUrl);
    fixture.executePutRequest(TEST_URL, getTestObject(), null);
  }

  private String addSimpleCookieToResponse(MockLowLevelHttpResponse response) {
    String cookieOne = String.format("%s=%s", "cookiename", TEST_COOKIE);
    response.addHeader("Set-Cookie", cookieOne);
    return cookieOne;
  }

  private String addComplexCookieToResponse(MockLowLevelHttpResponse response) {
    String cookieTwo = String.format("%s=%s", "monster", TEST_COOKIE);
    response.addHeader("Set-Cookie", String.format("%s; Path=/some/dummy/", cookieTwo));
    return cookieTwo;
  }

  @Test
  public void executePostLoginRequest_shouldReturnCookie() throws Exception {
    MockLowLevelHttpResponse response = successfulResponse();
    String cookieOne = addSimpleCookieToResponse(response);
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestWithSuccessfulResponse(response));

    List<String> cookieHeaders = fixture.executePostLoginRequest(TEST_URL, getTestObject());

    assertEquals(cookieHeaders, Arrays.asList(cookieOne));
  }

  @Test
  public void executePostLoginRequest_shouldReturnCookiesWithoutExtraStuff() throws Exception {
    MockLowLevelHttpResponse response = successfulResponse();
    String cookieTwo = addComplexCookieToResponse(response);
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestWithSuccessfulResponse(response));

    List<String> cookieHeaders = fixture.executePostLoginRequest(TEST_URL, getTestObject());

    assertEquals(cookieHeaders, Arrays.asList(cookieTwo));
  }

  @Test
  public void executePostLoginRequest_shouldReturnMultipleCookies() throws Exception {
    MockLowLevelHttpResponse response = successfulResponse();
    String cookieOne = addSimpleCookieToResponse(response);
    String cookieTwo = addComplexCookieToResponse(response);
    whenHttpRequest_ReturnHttpResponse(createMockHttpRequestWithSuccessfulResponse(response));

    List<String> cookieHeaders = fixture.executePostLoginRequest(TEST_URL, getTestObject());

    assertEquals(cookieHeaders, Arrays.asList(cookieOne, cookieTwo));
  }

  private TestObject getTestObject() {
    return new TestObject("one", "two");
  }

  private String getTestObjectAsJson() {
    return "{\"field1\":\"one\",\"field2\":\"two\"}";
  }

  public static class TestObject {
    @Key
    private String field1;

    @Key
    private String field2;

    @SuppressWarnings("unused")
    public TestObject() {
    }

    public TestObject(String field1, String field2) {
      this.field1 = field1;
      this.field2 = field2;
    }

    @SuppressWarnings("unused")
    public String getField1() {
      return field1;
    }

    @SuppressWarnings("unused")
    public void setField1(String field1) {
      this.field1 = field1;
    }

    @SuppressWarnings("unused")
    public String getField2() {
      return field2;
    }

    @SuppressWarnings("unused")
    public void setField2(String field2) {
      this.field2 = field2;
    }

    @Override // Auto-generated by IntelliJ, included to simplify test case assertions
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof TestObject)) return false;

      TestObject that = (TestObject) o;

      if (field1 != null ? !field1.equals(that.field1) : that.field1 != null) return false;
      if (field2 != null ? !field2.equals(that.field2) : that.field2 != null) return false;

      return true;
    }

    @Override // Auto-generated by IntelliJ, included to make test assertion failure messages more meaningful
    public String toString() {
      return "TestObject{" +
          "field1='" + field1 + '\'' +
          ", field2='" + field2 + '\'' +
          '}';
    }
  }

}

