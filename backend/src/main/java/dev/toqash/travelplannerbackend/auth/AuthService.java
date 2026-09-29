package dev.toqash.travelplannerbackend.auth;

import dev.toqash.travelplannerbackend.auth.dto.LoginRequest;
import dev.toqash.travelplannerbackend.user.User;
import dev.toqash.travelplannerbackend.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager){
        this.authenticationManager = authenticationManager;
    }

    public void login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
    }
}
