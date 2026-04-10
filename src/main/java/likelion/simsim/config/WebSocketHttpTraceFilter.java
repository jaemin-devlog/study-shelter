package likelion.simsim.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * /ws 계열 요청이 앱까지 실제로 들어오는지 추적한다.
 */
@Component
@Profile("!prod")
public class WebSocketHttpTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHttpTraceFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || (!uri.startsWith("/ws") && !uri.startsWith("/ws-native"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String uri = request.getRequestURI();

        log.debug(
                "WS_HTTP_IN method={} uri={} query={} upgrade={} connection={} accept={} xfp={} ua={}",
                request.getMethod(),
                uri,
                request.getQueryString(),
                request.getHeader("Upgrade"),
                request.getHeader("Connection"),
                request.getHeader("Accept"),
                request.getHeader("X-Forwarded-Proto"),
                request.getHeader("User-Agent")
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug(
                    "WS_HTTP_OUT method={} uri={} status={} durationMs={}",
                    request.getMethod(),
                    uri,
                    response.getStatus(),
                    System.currentTimeMillis() - startedAt
            );
        }
    }
}
