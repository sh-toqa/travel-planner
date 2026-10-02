package dev.toqash.travelplannerbackend.auth;

import dev.toqash.travelplannerbackend.user.User;
import dev.toqash.travelplannerbackend.user.UserRepository;
import dev.toqash.travelplannerbackend.user.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.email()),
                        request.password()
                )
        );
    }

    @Transactional
    public UserResponse register(RegisterRequest request){
        String email = normalizeEmail(request.email());
        String displayName = normalizeDisplayName(request.displayName());

        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyUsedException();
        }

        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .password(passwordEncoder.encode(request.password()))
                .build();

        User saved;
        try {
            // Flush now so a concurrent duplicate hits the unique index inside this try block.
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyUsedException();
        }
        return UserResponse.from(saved);
    }

    private String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName){
        if(displayName == null || displayName.isBlank()){
            return null;
        }
        return displayName.trim();
    }
}