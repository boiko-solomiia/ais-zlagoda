package ua.kma.aiszlagoda.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/static/**", "/static/images/**").permitAll()

                        // profile / info about self
                        .requestMatchers("/profile").hasAnyRole("MANAGER", "CASHIER")

                        // cashier only: creating checks
                        .requestMatchers("/check/add").hasRole("CASHIER")

                        // manager only: employee management and reports
                        .requestMatchers(
                                "/employee",
                                "/employee/add", "/employee/edit/**", "/employee/delete/**",
                                "/employee/search", "/employee/filter", "/employee/print"
                        ).hasRole("MANAGER")

                        // manager only: category management and reports
                        .requestMatchers(
                                "/category",
                                "/category/add", "/category/edit/**", "/category/delete/**",
                                "/category/print"
                        ).hasRole("MANAGER")

                        // both can view/search products
                        .requestMatchers(
                                "/product",
                                "/product/search", "/product/filter"
                        ).hasAnyRole("MANAGER", "CASHIER")

                        // manager only: product management, print, sales analytics
                        .requestMatchers(
                                "/product/add", "/product/edit/**", "/product/delete/**",
                                "/product/print", "/product/sold-quantity"
                        ).hasRole("MANAGER")

                        // both can view/search store products
                        .requestMatchers(
                                "/store-product",
                                "/store-product/search", "/store-product/filter",
                                "/store-product/info/**"
                        ).hasAnyRole("MANAGER", "CASHIER")

                        // manager only: store product management and print
                        .requestMatchers(
                                "/store-product/add", "/store-product/edit/**", "/store-product/delete/**",
                                "/store-product/print"
                        ).hasRole("MANAGER")

                        // both can view/add/edit customer cards
                        .requestMatchers(
                                "/customer-card",
                                "/customer-card/search", "/customer-card/filter",
                                "/customer-card/add", "/customer-card/edit/**"
                        ).hasAnyRole("MANAGER", "CASHIER")

                        // manager only: delete and print customer cards
                        .requestMatchers(
                                "/customer-card/delete/**", "/customer-card/print"
                        ).hasRole("MANAGER")

                        // both can view/search/filter checks
                        .requestMatchers(
                                "/check",
                                "/check/search", "/check/filter", "/check/info/**"
                        ).hasAnyRole("MANAGER", "CASHIER")

                        // manager only: delete and print checks
                        .requestMatchers(
                                "/check/delete/**", "/check/print"
                        ).hasRole("MANAGER")

                        // all authenticated users
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .csrf(Customizer.withDefaults());

        return http.build();
    }
}