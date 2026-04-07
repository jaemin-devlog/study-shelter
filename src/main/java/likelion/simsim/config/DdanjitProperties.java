package likelion.simsim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml 값을 타입 안전하게 꺼내기 위한 설정 객체입니다.
 */
@Component
@ConfigurationProperties(prefix = "ddanjit")
public class DdanjitProperties {

    private long sessionTtlSeconds = 7200L;
    private long heartbeatIntervalMillis = 10000L;
    private long staleThresholdMillis = 30000L;
    private long cleanupFixedDelayMillis = 15000L;

    public long getSessionTtlSeconds() {
        return sessionTtlSeconds;
    }

    public void setSessionTtlSeconds(long sessionTtlSeconds) {
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    public long getHeartbeatIntervalMillis() {
        return heartbeatIntervalMillis;
    }

    public void setHeartbeatIntervalMillis(long heartbeatIntervalMillis) {
        this.heartbeatIntervalMillis = heartbeatIntervalMillis;
    }

    public long getStaleThresholdMillis() {
        return staleThresholdMillis;
    }

    public void setStaleThresholdMillis(long staleThresholdMillis) {
        this.staleThresholdMillis = staleThresholdMillis;
    }

    public long getCleanupFixedDelayMillis() {
        return cleanupFixedDelayMillis;
    }

    public void setCleanupFixedDelayMillis(long cleanupFixedDelayMillis) {
        this.cleanupFixedDelayMillis = cleanupFixedDelayMillis;
    }
}
