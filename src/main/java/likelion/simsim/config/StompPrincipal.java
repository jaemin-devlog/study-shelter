package likelion.simsim.config;

import java.security.Principal;

/**
 * STOMP 세션에 닉네임을 Principal 로 넣기 위한 간단한 구현체입니다.
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
