package com.vocaldocs.controllers;

import com.vocaldocs.models.User;
import com.vocaldocs.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    @Autowired
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestParam("username") String username, 
            @RequestParam("email") String email, 
            @RequestParam("password") String password,
            HttpSession session) {
        Map<String, String> response = new HashMap<>();

        if (userRepository.findByEmail(email).isPresent()) {
            response.put("error", "Email already in use.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            response.put("error", "Username already in use.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User newUser = new User(email, username, password);
        userRepository.save(newUser);

        session.setAttribute("userId", newUser.getId());
        session.setAttribute("userEmail", newUser.getEmail());
        session.setAttribute("username", newUser.getUsername());

        response.put("message", "Registration successful.");
        response.put("username", newUser.getUsername());
        response.put("email", newUser.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam("email") String credential, @RequestParam("password") String password,
            HttpSession session) {
        Map<String, String> response = new HashMap<>();

        Optional<User> userOptional = userRepository.findByEmail(credential);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(credential);
        }

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) {
                // Login successful
                session.setAttribute("userId", user.getId());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("username", user.getUsername());

                response.put("message", "Login successful.");
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            }
        }

        response.put("error", "Invalid email or password.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            response.put("loggedIn", true);
            response.put("email", (String) session.getAttribute("userEmail"));
            response.put("username", (String) session.getAttribute("username"));
            return ResponseEntity.ok(response);
        }
        response.put("loggedIn", false);
        return ResponseEntity.ok(response);
    }
}
