package likelion.simsim.auth;

import jakarta.validation.Valid;
import likelion.simsim.auth.dto.AuthMeResponse;
import likelion.simsim.auth.dto.AuthResponse;
import likelion.simsim.auth.dto.LoginRequest;
import likelion.simsim.auth.dto.SignupRequest;
import likelion.simsim.common.ApiResponse;
import likelion.simsim.common.AuthConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 REST API 입니다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionTokenResolver sessionTokenResolver;

    public AuthController(AuthService authService, SessionTokenResolver sessionTokenResolver) {
        this.authService = authService;
        this.sessionTokenResolver = sessionTokenResolver;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인에 성공했습니다.", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(
            @RequestHeader(value = AuthConstants.AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @RequestHeader(value = AuthConstants.SESSION_TOKEN_HEADER, required = false) String sessionTokenHeader
    ) {
        String sessionToken = sessionTokenResolver.resolveOrThrow(authorizationHeader, sessionTokenHeader);
        AuthMeResponse response = authService.me(sessionToken);
        return ResponseEntity.ok(ApiResponse.ok("현재 로그인 정보를 조회했습니다.", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = AuthConstants.AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @RequestHeader(value = AuthConstants.SESSION_TOKEN_HEADER, required = false) String sessionTokenHeader
    ) {
        String sessionToken = sessionTokenResolver.resolveOrThrow(authorizationHeader, sessionTokenHeader);
        authService.logout(sessionToken);
        return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다."));
    }
}
