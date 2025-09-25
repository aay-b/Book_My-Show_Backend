package com.bookmyshow.Controllers;

import com.bookmyshow.Enums.Role;
import com.bookmyshow.Models.User;
import com.bookmyshow.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/signup")
public class AuthController {

    @Autowired
    private UserService userService; // Injects your UserService bean. The controller will delegate to it for real work.

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String name = request.get("name");
            String gender = request.get("gender");
            String ageStr = request.get("age");
            String phoneNumber = request.get("phoneNumber");
            String email = request.get("email");
            String roleStr = request.get("role");

            if (username == null || password == null || name == null || gender == null ||
                    ageStr == null || phoneNumber == null || email == null) {
                return new ResponseEntity<>("All fields are required", HttpStatus.BAD_REQUEST);
            }

            Integer age = Integer.parseInt(ageStr);
            // Converts age from string to int; throws NumberFormatException if invalid (caught by catch below).


            // now obviously in our project, this is done for simplicity while learning — so we can test both flows
            // (normal user vs. admin) easily. If we type admin, we become admin - simple
            // Obviously this is not done in real-world apps.

            Role role = Role.USER; // Defaults role to USER
            if (roleStr != null && roleStr.equalsIgnoreCase("ADMIN")) {
                role = Role.ADMIN; // If client sent "ADMIN" (case-insensitive), role becomes ADMIN
            }

            String result = userService.registerUser(username, password, name, gender, age,
                    phoneNumber, email, Set.of(role));
            // Set.of() -> This allows flexibility to assign more than one role later.
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            if (username == null || password == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Username and password are required");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            String token = userService.verifyUser(username, password);
            /*
                -- This calls the service layer to check if the user exists and the password is correct.
                -- If everything is fine → the service returns a token (a JWT token).
                -- A token is just a long string that proves you’re logged in.
                -- The frontend/client will store this token (usually in localStorage or a cookie) and send it along with
                every request.
                -- On the server side, Spring Security will validate that token to confirm ->
                “yes, this request is from a real logged-in user”.
                -- Without this token, our API wouldn’t know if someone is authenticated after login.
            */

            Map<String, String> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("username", username);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Spring Security stores the currently logged-in user’s authentication info in something called the SecurityContext.
        // This line pulls that info out.
        // If the user is logged in, authentication will contain details like:
        // principal → the logged-in user (usually a UserDetails object)
        // authorities → their roles (e.g. USER, ADMIN)
        // authenticated → whether they are actually authenticated (true/false).

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            // authentication.getPrincipal() instanceof String -> avoids cases where Spring stores "anonymousUser"
            // (a placeholder string when no one is logged in). First 2 are self-explanatory
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        // Now we’re confident a real user is logged in.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // authentication.getPrincipal() gives us the logged-in user.
        // We cast it to UserDetails (Spring’s standard user object that contains username, password (encrypted), and authorities).

        User user = userService.findByUsername(userDetails.getUsername());
        // fetches the username and looks up the full User entity from our database.

        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            // If the user somehow isn’t found in the database → return 404 Not Found.

        }

        // // This commented-out code shows an alternative response → instead of returning the full User entity, we could return only username + roles in JSON.

        // Map<String, Object> response = new HashMap<>();
        // response.put("username", authentication.getName());
        // response.put("roles", authentication.getAuthorities());

        return new ResponseEntity<>(user, HttpStatus.OK);
        // Finally, return the full User object in the response body with a 200 OK status.
        // The frontend can now display the user’s profile info.
    }
}