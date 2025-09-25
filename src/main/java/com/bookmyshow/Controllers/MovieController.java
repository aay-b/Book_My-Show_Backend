// MovieController.java
package com.bookmyshow.Controllers;

import com.bookmyshow.Dtos.RequestDtos.MovieEntryDto;

import com.bookmyshow.Models.Movie;
import com.bookmyshow.Services.MovieService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController // marks this class as a REST API controller
@RequestMapping("/movies") // Sets a base URL for all endpoints in this controller.
public class MovieController {

    @Autowired // @Autowired tells Spring to inject an instance of MovieService here so you don’t
    // need "new MovieService()" manually; Spring manages it.
    private MovieService movieService;

    @PostMapping("/add") // This method handles POST /movies/add
    public ResponseEntity<String> addMovie(@RequestBody @Valid MovieEntryDto movieEntryDto) {
        // @RequestBody Takes JSON from the request body and maps it to MovieEntryDto
        // @Valid → Runs validation (if MovieEntryDto has rules like @NotBlank, @Size, etc.)
        String response = movieService.addMovie(movieEntryDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
        // Wraps the response string with status 201 CREATED.
    }
    
    
    @GetMapping("/{name}")
    public ResponseEntity<Movie> getMovieByName(@PathVariable String name) {
        // @PathVariable String name - Extracts the {name} part from the URL.
        Movie movie = movieService.getMovieByName(name);
        return new ResponseEntity<>(movie, HttpStatus.OK);
        // Returns the Movie object as JSON with status 200 OK
    }
    
    @GetMapping("/totalCollection/{movieId}") // Returns total box office collection of movie with ID 1
    public ResponseEntity<Long> totalCollection(@PathVariable Integer movieId) {
        // @PathVariable Integer movieId - Extracts the {movieId} part from the URL.
        try {
            Long result = movieService.totalCollection(movieId);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return null;
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<Movie>> getAllMovies() {
        // Returns a list of all movies
        List<Movie> movieList = movieService.getAllMovies();
        return new ResponseEntity<>(movieList, HttpStatus.OK);
    }
    
    @GetMapping("/id/{id}") // GET movie by Id
    public ResponseEntity<Movie> getMovieById(@PathVariable Integer id) {
        // @PathVariable Integer id - Extracts the {id} part from the URL.
        Movie movie = movieService.getMovieById(id);
        return new ResponseEntity<>(movie, HttpStatus.OK);
    }

    @DeleteMapping("/{id}") // DELETE movie by Id
    public ResponseEntity<String> deleteMovie(@PathVariable Integer id) {
        movieService.deleteMovie(id);
        return new ResponseEntity<>("Movie deleted successfully", HttpStatus.OK);
    }

    @PutMapping("/{id}") // PUT/Update movie by Id
    public ResponseEntity<String> updateMovie(@PathVariable Integer id,
                                              @RequestBody @Valid MovieEntryDto movieEntryDto) {
        movieService.updateMovie(id, movieEntryDto);
        return new ResponseEntity<>("Movie updated successfully", HttpStatus.OK);
    }
    
    @GetMapping("/search") // search movie by name
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String name) {
        // RequestParam binds query parameters from the URL to your method arguments.
        // Query parameters are the ?key=value parts of a URL.
        List<Movie> movies = movieService.searchMoviesByName(name);
        return ResponseEntity.ok(movies); // Returns list of matching movies.
    }

    /*
    *NOTE*:
            @PathVariable = value inside the path (e.g., /movies/123)
            @RequestParam = value in query string (e.g., /movies/search?name=Inception)
     */

}
