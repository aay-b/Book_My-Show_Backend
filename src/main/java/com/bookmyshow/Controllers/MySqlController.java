package com.bookmyshow.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController // Marks this class as a REST controller that returns JSON.
/*  remember:
        REST is a way for clients (like your React frontend or Postman) and servers (your Spring Boot backend)
        to talk to each other over HTTP, using simple and consistent rules.
*/
@RequestMapping("/mysql")
public class MySqlController {

    @Autowired
    private JdbcTemplate jdbcTemplate; // Injects Spring’s JdbcTemplate bean (configured automatically
    // because we have a database connection in our application.properties).
    // This object lets you execute raw SQL queries directly against your database.

    @GetMapping("/query")
    // ResponseEntity = a Spring wrapper that lets you control body + status + headers in your response
    public ResponseEntity<?> executeQuery(@RequestParam("query") String query) {
        // For safety, this example only allows SELECT queries.
        // A true "universal" runner would be a major security risk.
        if (!query.trim().toLowerCase().startsWith("select")) {
            /*
            -- This check ensures only SELECT queries are allowed.
            -- Why? Because if you allowed INSERT, UPDATE, DELETE, or DROP TABLE, someone could
            wipe your database by calling this API.
             */
            return new ResponseEntity<>("Only SELECT queries are allowed for security reasons.", HttpStatus.FORBIDDEN);
            /*
            If the query doesn’t start with "select", return:
                -- HTTP status 403 Forbidden
                -- Message: "Only SELECT queries are allowed...".
             */
        }

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
            /*
            -- jdbcTemplate.queryForList(query) runs the SQL query
            -- Returns a List<Map<String, Object>>
                -- Each row = one Map
                -- Each column in that row = key:value entry in the map
            -- Wraps the result in ResponseEntity with status 200 OK.
             */
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    /*
    -- This controller is basically a SQL query runner exposed via REST.
    -- Very powerful ⚡ but also dangerous (that’s why only SELECT is allowed).
    -- Normally, you wouldn’t give direct SQL query access to users—it’s more for debugging/testing.
     */
}
