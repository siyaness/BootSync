package com.bootsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        ActiveMemberSessionFilter activeMemberSessionFilter,
        AuthRateLimitService authRateLimitService,
        ClientIpResolver clientIpResolver,
        @Value("${server.servlet.session.cookie.name:BOOTSYNCSESSION}") String sessionCookieName
    ) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/app/dashboard");
        successHandler.setAlwaysUseDefaultTargetUrl(false);

        AuthenticationFailureHandler failureHandler = (request, response, exception) -> {
            authRateLimitService.recordLoginFailure(request.getParameter("username"));
            response.sendRedirect(request.getContextPath() + "/app/login?error");
        };

        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/", "/login", "/signup", "/actuator/health").permitAll()
                .requestMatchers("/app", "/app/**").permitAll()
                .requestMatchers("/auth/login", "/auth/recovery-email/verify").permitAll()
                .requestMatchers("/api/auth/session", "/api/auth/login", "/api/auth/signup").permitAll()
                .requestMatchers("/api/recovery-email/preview", "/api/recovery-email/confirm").permitAll()
                .anyRequest().authenticated()
            )
            .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
            .sessionManagement(session -> session.sessionFixation(sessionFixation -> sessionFixation.migrateSession()))
            .addFilterBefore(new LoginRateLimitFilter(authRateLimitService, clientIpResolver), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(activeMemberSessionFilter, AnonymousAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/app/login")
                .loginProcessingUrl("/auth/login")
                .successHandler((request, response, authentication) -> {
                    authRateLimitService.clearLoginFailures(authentication.getName());
                    successHandler.onAuthenticationSuccess(request, response, authentication);
                })
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/app/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies(sessionCookieName, "JSESSIONID")
            );
        return http.build();
    }
}
