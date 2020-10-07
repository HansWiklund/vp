package se.skl.tp.vp.integrationtests;

import static org.junit.Assert.assertEquals;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.util.TestLogAppender;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(properties = {
    "throttle.client.maxRequests=3",
    "throttle.client.timePeriodMillis=2000"
})
@StartTakService
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FullServiceClientThrottleTest {

  public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockProducer;

  @Autowired
  MockProducer mockHttpsProducer;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  @Before
  public void before() {
    try {
      mockHttpsProducer.start(HTTPS_PRODUCER_URL + "?sslContextParameters=#outgoingSSLContextParameters&ssl=true");
    } catch (Exception e) {
      e.printStackTrace();
    }
    testLogAppender.clearEvents();
  }

  @Test
  public void clientThrottleShouldKickInAfterThreeFastCalls() throws InterruptedException {

    Map<String, Object> sender1 = createHeaders("tp");
    Map<String, Object> sender2 = createHeaders("tp2");

    final MockEndpoint resultEndpoint = testConsumer.getResultEndpoint();

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(500, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender2);
    assertEquals(200, getResponseCode(resultEndpoint));

    TimeUnit.SECONDS.sleep(2);

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(200, getResponseCode(resultEndpoint));

  }

  @Test
  public void clientThrottleShouldNotKickInAfterDifferentCallers() throws InterruptedException {

    Map<String, Object> sender1 = createHeaders("tp");
    Map<String, Object> sender2 = createHeaders("tp2");
    Map<String, Object> sender3 = createHeaders("tp3");

    final MockEndpoint resultEndpoint = testConsumer.getResultEndpoint();

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender1);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender2);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender2);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender3);
    assertEquals(200, getResponseCode(resultEndpoint));

    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), sender3);
    assertEquals(200, getResponseCode(resultEndpoint));
  }

  private Map<String, Object> createHeaders(String senderId) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, senderId);
    return headers;
  }

  private int getResponseCode(MockEndpoint resultEndpoint) {
    return resultEndpoint.getExchanges().get(0).getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
  }

}
