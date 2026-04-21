package ua.kma.aiszlagoda.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.AuthUser;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.UserAccountService;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    public CustomUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Response<AuthUser> response = userAccountService.findAuthUserByUsername(username);

        AuthUser authUser = response.getObject();
        if (authUser == null) {
            throw new UsernameNotFoundException("User not found");
        }

        String role = switch (authUser.getRole().toLowerCase()) {
            case "manager", "менеджер" -> "MANAGER";
            case "cashier", "касир" -> "CASHIER";
            default -> throw new UsernameNotFoundException("Unknown role: " + authUser.getRole());
        };

        return User.builder()
                .username(authUser.getUsername())
                .password(authUser.getPasswordHash())
                .roles(role)
                .build();
    }
}