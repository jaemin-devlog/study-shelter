package likelion.simsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Redis 연결 자체는 Spring Boot 자동 설정을 사용하고,
 * 여기서는 BCrypt 인코더만 별도로 등록합니다.
 */
@Configuration
public class RedisConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
