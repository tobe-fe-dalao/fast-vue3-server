package com.fastvue.security;

import static org.assertj.core.api.Assertions.*;

import com.fastvue.config.CorsProperties;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class SecurityCollectionOwnershipTest {
    @Test
    void callerCannotChangeAnAuthenticatedUsersRolesOrAuthorities() {
        var roles = new ArrayList<>(List.of("reader"));
        var permissions = new ArrayList<>(List.of("article:list"));
        List<GrantedAuthority> authorities =
                new ArrayList<>(List.of(new SimpleGrantedAuthority("article:list")));
        var user = new LoginUser(1L, 1L, "reader", "unused", true, roles, permissions, authorities);
        roles.add("admin");
        permissions.add("user:delete");
        authorities.clear();
        assertThat(user.roles()).containsExactly("reader");
        assertThat(user.permissions()).containsExactly("article:list");
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("article:list");
        assertThatThrownBy(() -> user.roles().add("admin"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> user.permissions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> user.authorities().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> user.getAuthorities().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void corsBindingOwnsItsOriginSnapshot() {
        var origins = new ArrayList<>(List.of("https://app.example.com"));
        var properties = new CorsProperties(origins);
        origins.add("https://untrusted.example.com");
        assertThat(properties.allowedOriginPatterns()).containsExactly("https://app.example.com");
        assertThatThrownBy(() -> properties.allowedOriginPatterns().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void missingCorsOriginsStillReachBeanValidationAsNull() {
        assertThat(new CorsProperties(null).allowedOriginPatterns()).isNull();
    }
}
