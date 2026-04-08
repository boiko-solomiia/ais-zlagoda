package ua.kma.aiszlagoda.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public PasswordService(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public String encodePassword(String password) {
        return bCryptPasswordEncoder.encode(password);
    }

    public boolean compareRawAndEncodedPassword(String raw, String encoded) {
        return bCryptPasswordEncoder.matches(raw, encoded);
    }
}
