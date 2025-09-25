package com.bookmyshow.Controllers;

import com.bookmyshow.Models.Review;
import com.bookmyshow.Services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired // injects the ReviewService bean created elsewhere
    private ReviewService reviewService;

    @PostMapping("/add")// Add a review (authenticated)
    public ResponseEntity<Review> addReview(@RequestBody Map<String, String> request, Authentication authentication) {

        /* -- @RequestBody Map<String,String> → expects a JSON object in the request body (e.g.,
         {"movieId":"1","rating":"5","comment":"Great!"}).
           -- Authentication authentication → Spring Security injects the current user’s auth object.
         */

        // Get the currently authenticated user
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // authentication.getPrincipal() returns the user object tied to the current session/token.
        // Cast to UserDetails so you can read username, authorities, etc.
        
        Integer movieId = Integer.parseInt(request.get("movieId"));
        Integer rating = Integer.parseInt(request.get("rating"));
        String comment = request.get("comment");

        Review review = reviewService.addReview(movieId, rating, comment, userDetails);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @GetMapping("/movie/{movieId}") // get reviews of a movie by its Id
    public ResponseEntity<List<Review>> getReviewsByMovie(@PathVariable Integer movieId) {
        // @PathVariable extracts movieId from the URL.
        List<Review> reviews = reviewService.getReviewsByMovie(movieId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
        // returns 200 OK with a list of reviews for that movie.
    }

    @PutMapping("/update/{reviewId}") // Update a review (only by the owner)
    public ResponseEntity<Review> updateReview(@PathVariable Integer reviewId, @RequestBody Map<String, String> request, Authentication authentication) {

        // Authentication authentication - Provided by Spring Security. Holds information about the currently logged-in user
        // (from their JWT/session/etc.).


        /*
        In plain words:
        1. User (logged-in) calls PUT /reviews/update/{id} with new rating & comment in body.
        2. Controller extracts:
            - Which review to update (reviewId from URL).
            - New rating & comment (request body).
            - Who is making the request (authentication).
        3. Controller passes all that to the service.
        4. Service checks ownership + updates DB.
        5. Updated review comes back, and controller returns it with 200 OK.
        */

        // Get the currently authenticated user
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Gets data out of the JSON body (Map).
        // Example: if body was {"rating": "5", "comment": "Awesome!"}, now rating = 5, comment = "Awesome!".
        Integer rating = Integer.parseInt(request.get("rating"));
        String comment = request.get("comment");

        Review updatedReview = reviewService.updateReview(reviewId, rating, comment, userDetails);

        /* Calls the service layer to do the real work:
                -- Find the review in the database (by reviewId).
                -- Check if the userDetails (current user) is allowed to update it (ownership check).
                -- Update the rating and comment.
                -- Save it back to DB.
        */

        // Returns the updated Review object.
        return new ResponseEntity<>(updatedReview, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{reviewId}") // Delete a review (only by the owner)
    public ResponseEntity<String> deleteReview(@PathVariable Integer reviewId, Authentication authentication) {
        // secured endpoint; authenticated user is injected.
        // Get the currently authenticated user
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // service should check ownership/permissions before deletion.
        reviewService.deleteReview(reviewId, userDetails);
        return new ResponseEntity<>("Review deleted successfully", HttpStatus.OK);
    }
}