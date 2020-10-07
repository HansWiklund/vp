package se.skl.tp.vp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "throttle")
public class ThrottlingProperties {
  private int maxInflightExchanges;
  private int resumePercentOfMax;
  private ClientThrottle client;

  @Data
  public static class ClientThrottle {
    private String correlationKey;
    private int maxRequests;
    private int timePeriodMillis;
    private boolean rejectExecution;
  }

}
