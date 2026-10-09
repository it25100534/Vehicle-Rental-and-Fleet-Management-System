package com.example.vehiclerentalserviceplatform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashTests {

    @Test
    void hashesAndVerifiesPasswordsWithoutSavingPlainText() {
        String encoded = PasswordHash.encode("Secure123");

        assertThat(encoded).startsWith("pbkdf2$").doesNotContain("Secure123");
        assertThat(PasswordHash.matches("Secure123", encoded)).isTrue();
        assertThat(PasswordHash.matches("Wrong123", encoded)).isFalse();
    }

    @Test
    void safelyRejectsMalformedHashes() {
        assertThat(PasswordHash.matches("password", "pbkdf2$broken$value")).isFalse();
        assertThat(PasswordHash.matches(null, "anything")).isFalse();
    }
}
