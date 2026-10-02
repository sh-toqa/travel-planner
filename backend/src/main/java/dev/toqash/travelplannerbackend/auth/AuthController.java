package dev.toqash.travelplannerbackend.auth;

import dev.toqash.travelplannerbackend.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Registration and login")
@SecurityRequirements // public: no credentials needed
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @Operation(summary = "Check email and password")
    @PostMapping("/login")
    public void login(@Valid @RequestBody LoginRequest request){
        authService.login(request);
    }

    @Operation(summary = "Create an account", description = "Emails are case-insensitive; a taken email returns 409 EMAIL_TAKEN.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request); }
}
