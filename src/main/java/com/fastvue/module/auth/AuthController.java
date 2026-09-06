package com.fastvue.module.auth;

import com.fastvue.common.ApiResponse;
import com.fastvue.module.user.CreateUserRequest;
import com.fastvue.module.user.UserService;
import com.fastvue.module.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@Tag(name = "认证", description = "登录 / 登出 / 刷新令牌 / 当前用户")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        CreateUserRequest createRequest = new CreateUserRequest(
                request.username(), request.password(), request.username(), request.email(), null);
        return ApiResponse.success(userService.create(createRequest));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(authService.me());
    }
}
