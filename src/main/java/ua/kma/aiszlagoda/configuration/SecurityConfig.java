package ua.kma.aiszlagoda.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // всі запити дозволені
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable());
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/static/**", "/static/images/**").permitAll()
//                        .requestMatchers("/manager/**").hasRole("MANAGER")
//                        .requestMatchers("/cashier/**").hasAnyRole("CASHIER", "MANAGER")
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .loginProcessingUrl("/login")
//                        .defaultSuccessUrl("/", true)
//                        .failureUrl("/login?error=true")
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/login?logout=true")
//                        .permitAll()
//                )
//                .csrf(Customizer.withDefaults());

        return http.build();
    }
}