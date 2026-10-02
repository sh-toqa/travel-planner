package dev.toqash.travelplannerbackend.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public UserController(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public UserResponse me() {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return UserResponse.from(user);
    }
}