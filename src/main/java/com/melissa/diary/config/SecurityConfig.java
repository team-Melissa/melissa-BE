package com.melissa.diary.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) {
        try {

            http.csrf(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .cors(Customizer.withDefaults())
                    .sessionManagement((sessionManagement) ->
                            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )

                    .authorizeHttpRequests((authorizeRequests) ->
                            authorizeRequests.requestMatchers("/health","/api/v1/auth/**",
                                            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/test/**").permitAll()

                                    .anyRequest().authenticated()
                    )
                    .exceptionHandling((exceptionConfig) ->
                            exceptionConfig
                                    .authenticationEntryPoint(unauthorizedEntryPoint)
                    ); // 401 403 관련 예외처리
            ;
            http.addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );
            return http.build();
        } catch (Exception e) {

            throw new RuntimeException(e);
        }

    }

    private final AuthenticationEntryPoint unauthorizedEntryPoint =
            (request, response, authException) -> {

                ApiResponse<?> apiResponse = ApiResponse.onFailure("401", "인증이 필요합니다.", null);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                PrintWriter writer = response.getWriter();
                writer.write(new ObjectMapper().writeValueAsString(apiResponse));
                writer.flush();

            };

    /**
     * Spring Security의 기본 InMemoryUserDetailsManager 자동 생성을 비활성화
     * JWT 기반 인증만 사용하므로 UserDetailsService는 필요하지 않음
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("JWT 인증만 사용합니다. UserDetailsService는 비활성화되어 있습니다.");
        };
    }
}