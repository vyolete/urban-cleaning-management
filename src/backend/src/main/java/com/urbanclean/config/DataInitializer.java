package com.urbanclean.config;

import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Initializes default users in the database on application startup
 * Only creates users if the database is empty
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only initialize if database is empty
        if (userRepository.count() == 0) {
            log.info("Database is empty. Creating default users...");
            createDefaultUsers();
            log.info("Default users created successfully");
        } else {
            log.info("Database already contains users. Skipping initialization.");
        }
    }

    private void createDefaultUsers() {
        // Create admin user
        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("Admin123!@#"))
                .email("admin@urbanclean.com")
                .role(UserRole.ROLE_ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .anonymized(false)
                .tokenVersion(0)
                .build();
        userRepository.save(admin);
        log.info("Created admin user: {}", admin.getUsername());

        // Create tecnico user
        User tecnico = User.builder()
                .username("tecnico")
                .passwordHash(passwordEncoder.encode("Tecnico123!@#"))
                .email("tecnico@urbanclean.com")
                .role(UserRole.ROLE_TECNICO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .anonymized(false)
                .tokenVersion(0)
                .build();
        userRepository.save(tecnico);
        log.info("Created tecnico user: {}", tecnico.getUsername());

        // Create ciudadano user
        User ciudadano = User.builder()
                .username("ciudadano")
                .passwordHash(passwordEncoder.encode("Ciudadano123!@#"))
                .email("ciudadano@urbanclean.com")
                .role(UserRole.ROLE_CIUDADANO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .anonymized(false)
                .tokenVersion(0)
                .build();
        userRepository.save(ciudadano);
        log.info("Created ciudadano user: {}", ciudadano.getUsername());
    }
}
