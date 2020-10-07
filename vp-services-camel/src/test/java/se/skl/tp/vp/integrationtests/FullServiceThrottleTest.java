package se.skl.tp.vp.integrationtests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
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
    "throttle.maxInflightExchanges=10"
})
@StartTakService
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FullServiceThrottleTest {

  public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockHttpsProducer;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  private int corrId = 0;

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
  public void throttleShouldKickIn() throws InterruptedException, ExecutionException {

    // MaxInflight=10
    // 10 anrop startas/sekund
    // Varje anrop tar 500ms
    corrId = 0;
    mockHttpsProducer.setTimeout(500);
    load(() -> callVp(), 10, 5);

    final List<Exchange> exchanges = testConsumer.getResultEndpoint().getExchanges();

    long count503 = exchanges.stream().filter(ex -> ex.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).equals(503))
        .count();
    assertTrue(count503 < 20);

    long count200 = exchanges.stream().filter(ex -> ex.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).equals(200))
        .count();
    assertTrue(count200 > 30);

    // This should always work since the load is over
    testConsumer.getResultEndpoint().reset();
    callVp();
    assertEquals(1, testConsumer.getResultEndpoint().getExchanges().size());
    assertEquals(200L, (long) testConsumer.getResultEndpoint().getExchanges()
        .get(0).getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Long.class));

  }

  @Test
  public void throttleShouldNotKickIn() throws InterruptedException, ExecutionException {


    corrId = 0;
    mockHttpsProducer.setTimeout(0);
    load(() -> callVp(), 5, 10);

    final List<Exchange> exchanges = testConsumer.getResultEndpoint().getExchanges();

    long countFails = exchanges.stream().filter(ex -> !ex.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).equals(200))
        .count();
    assertEquals(0, countFails);
  }

  private void load(Runnable myTask, int ratePerSecond, int durationInSeconds) {

    final ExecutorService es = Executors.newCachedThreadPool();
    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    ses.scheduleAtFixedRate(() -> es.submit(myTask)
        , 0, 1000 / ratePerSecond, TimeUnit.MILLISECONDS);

    try {
      TimeUnit.SECONDS.sleep(durationInSeconds);
      ses.shutdown();
      es.shutdown();
      ses.awaitTermination(2, TimeUnit.SECONDS);
      es.awaitTermination(2, TimeUnit.SECONDS);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  private void callVp() {
    ++corrId;
    testConsumer
        .sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), createHeaders("tp"), false);
  }

  private Map<String, Object> createHeaders(String senderId) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_SKLTP_CORRELATION_ID, "" + corrId);
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, senderId);
    return headers;
  }


}
