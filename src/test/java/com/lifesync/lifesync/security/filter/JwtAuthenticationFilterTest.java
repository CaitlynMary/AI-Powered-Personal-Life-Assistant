package com.lifesync.lifesync.security.filter;

import com.lifesync.lifesync.security.service.CustomUserDetailsService;
import com.lifesync.lifesync.security.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void authenticatesValidBearerAndLeavesMissingTokenAnonymous() throws Exception {
        JwtService jwt = mock(JwtService.class);
        CustomUserDetailsService users = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);
        UserDetails details = User.withUsername("john@example.com").password("hash").authorities("ROLE_USER").build();
        when(jwt.extractSubject("valid-token")).thenReturn("john@example.com");
        when(jwt.isTokenValid("valid-token", details)).thenReturn(true);
        when(users.loadUserByUsername("john@example.com")).thenReturn(details);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer valid-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("john@example.com", SecurityContextHolder.getContext().getAuthentication().getName());

        SecurityContextHolder.clearContext();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
