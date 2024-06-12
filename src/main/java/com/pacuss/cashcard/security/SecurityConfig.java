package com.pacuss.cashcard.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static com.pacuss.cashcard.utility.Util.loadDetailsFromPropertiesFile;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(request -> request
                                .requestMatchers("/cash_cards/**")
                                .hasRole("CARD-OWNER") // Enable RBAC which handles authentication & authorization.
                        //.authenticated() // Does only authentication
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        User.UserBuilder users = User.builder();
        UserDetails owen = users
                .username(loadDetailsFromPropertiesFile("user-owen"))
                .password(passwordEncoder.encode(loadDetailsFromPropertiesFile("passwd-owen")))
                .roles("CARD-OWNER") // No roles for now
                .build();

        UserDetails kumar = users
                .username(loadDetailsFromPropertiesFile("user-kumar"))
                .password(passwordEncoder.encode(loadDetailsFromPropertiesFile("passwd-kumar")))
                .roles("CARD-OWNER") // No roles for now
                .build();

        UserDetails chris = users
                .username(loadDetailsFromPropertiesFile("user-chris"))
                .password(passwordEncoder.encode(loadDetailsFromPropertiesFile("passwd-chris")))
                .roles("NO-CARD") // No roles for now
                .build();

        return new InMemoryUserDetailsManager(owen, chris, kumar);
    }
}