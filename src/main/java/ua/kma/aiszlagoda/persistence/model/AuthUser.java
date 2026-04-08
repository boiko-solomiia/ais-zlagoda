package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private String username;
    private String passwordHash;
    private String idEmployee;
    private String role;
}