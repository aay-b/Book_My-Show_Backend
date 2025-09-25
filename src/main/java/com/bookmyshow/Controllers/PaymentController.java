package com.bookmyshow.Controllers;

import com.bookmyshow.Dtos.RequestDtos.TicketEntryDto;
import com.bookmyshow.Services.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils; // This import is correct
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin // allows CORS (Cross-Origin Resource Sharing). This is crucial if your frontend (React)
// runs on a different port/domain (e.g., React on 5173, backend on 8080). Without this, browsers would block
// requests.
public class PaymentController {

    @Value("${razorpay.key.id}") // @Value → injects values from application.properties file
    // These are our Razorpay API credentials in application.properties
    // same for keySecret below
    // these values are assigned in application.properties
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private TicketService ticketService;
    // After payment verification, this will be used to actually book the ticket.

    // This endpoint remains unchanged and is correct.
    @PostMapping("/create-order")
    public ResponseEntity<String> createOrder(@RequestBody Map<String, Object> data) {
        try {
            int amount = (int) data.get("amount");
            // Extract amount sent by frontend
            RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
            // Initializes Razorpay client with your API keys
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount * 100); // Multiply by 100 (because Razorpay expects pence, not rupees)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_receipt_" + System.currentTimeMillis());
            // receipt - unique string to identify this transaction
            Order order = razorpayClient.orders.create(orderRequest); // sends request to Razorpay servers
            return ResponseEntity.ok(order.toString()); // Returns the created order details as JSON string to frontend
        } catch (Exception e) {
            System.err.println("Error creating Razorpay order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Razorpay order.");
            // If anything goes wrong (invalid API keys, Razorpay down, etc.), return 500 Internal Server Error
        }
    }

    // This is the endpoint where the fix is applied.
    @PostMapping("/verify-payment")
    public ResponseEntity<String> verifyPaymentAndBookTicket(@RequestBody Map<String, Object> payload) {
        /* Frontend will send a payload containing:
                    -- razorpay_order_id
                    -- razorpay_payment_id
                    -- razorpay_signature (security check)
                    -- ticketEntryDto (ticket details: which movie, seats, user, etc.)
        */
        try {
            // Extracts values from the request JSON into variables.
            String razorpayOrderId = (String) payload.get("razorpay_order_id");
            String razorpayPaymentId = (String) payload.get("razorpay_payment_id");
            String razorpaySignature = (String) payload.get("razorpay_signature");
            Map<String, Object> ticketEntryMap = (Map<String, Object>) payload.get("ticketEntryDto");
            // ticketEntryDto is nested, so it’s first captured as a Map

            // --- THIS IS THE CORRECTED LOGIC ---
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            // Creates JSON with order/payment/signature

            boolean isSignatureVerified = Utils.verifyPaymentSignature(options, this.keySecret);
            // --- END OF CORRECTION ---
            /* Calls Utils.verifyPaymentSignature(...) from Razorpay SDK. This ensures that:
                        -- The payment is genuine
                        -- Signature matches → payment wasn’t tampered with.
            */

            if (!isSignatureVerified) {
                // If verification fails → reject request with 400 Bad Request
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed. Signature mismatch.");
            }

            // If signature is verified, proceed to book the ticket
            System.out.println("Payment verification successful. Booking ticket...");
            // Log success
            ObjectMapper mapper = new ObjectMapper();
            // Convert ticketEntryMap → TicketEntryDto using Jackson’s ObjectMapper.
            TicketEntryDto ticketEntryDto = mapper.convertValue(ticketEntryMap, TicketEntryDto.class);
            // Call ticketService.ticketBooking(...) → this books the ticket in your database.
            ticketService.ticketBooking(ticketEntryDto);

            return ResponseEntity.ok("Payment verified successfully. Ticket booked!");

        } catch (Exception e) {
            // Any error in verification/booking results in 500 Internal Server Error.
            System.err.println("Error in verifyPayment or ticket booking: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during payment verification or booking.");
        }
    }
    /*
    SUMMARY:

    -- Create Order → backend talks to Razorpay to generate an order ID, sends it to frontend.
    -- Verify Payment → backend checks Razorpay’s signature to confirm payment isn’t faked.
    -- If verified → ticket is booked via TicketService.

    -- This ensures:
        - No ticket is booked unless payment is real.
        - Security check prevents fraud (can’t just call API with fake payment ID).
     */
}
