package io.github.kinsleykajiva.demo;

import ecocash.api.EcocashClient;
import ecocash.api.EcocashException;
import ecocash.api.EcocashException.EcocashApiException;
import ecocash.api.EcocashException.PollTimeoutException;
import ecocash.api.InitPaymentResponse;
import ecocash.api.LookupTransactionResponse;
import ecocash.api.PollOptions;
import ecocash.api.PollStrategy;
import ecocash.api.RefundDetails;
import ecocash.api.RefundResponse;

/**
 * Demonstrates all major features of the Ecocash Java SDK.
 *
 * <p>Each scenario is isolated in its own static method so you can run
 * them independently or step through them in a debugger.</p>
 *
 * <p><strong>Before running:</strong> replace {@code YOUR_API_KEY} and
 * {@code YOUR_MERCHANT_CODE} with real sandbox credentials from the
 * <a href="https://developers.ecocash.co.zw">Ecocash Developer Portal</a>.</p>
 *
 * <h2>Scenarios covered</h2>
 * <ol>
 *   <li>Building a sandbox client</li>
 *   <li>Building a live client</li>
 *   <li>Initiating a payment</li>
 *   <li>Single transaction lookup (no polling)</li>
 *   <li>Polling — default INTERVAL strategy</li>
 *   <li>Polling — BACKOFF strategy with custom options</li>
 *   <li>Polling — SIMPLE (tight-loop) strategy</li>
 *   <li>Handling a poll timeout gracefully</li>
 *   <li>Refunding a payment</li>
 *   <li>Handling API errors</li>
 * </ol>
 */
public class DemoApp {
	
	// ------------------------------------------------------------------
	// Credentials — replace with your own sandbox values
	// ------------------------------------------------------------------
	
	private static final String API_KEY       = "YOUR_API_KEY";
	private static final String MERCHANT_CODE = "YOUR_MERCHANT_CODE";
	
	// A realistic Zimbabwean MSISDN used across examples
	private static final String CUSTOMER_PHONE = "26377000000";
	
	// ------------------------------------------------------------------
	// Entry point
	// ------------------------------------------------------------------
	
	public static void main(String[] args) {
		System.out.println("=== Ecocash Java SDK — Demo Application ===\n");
		
		demo1_buildSandboxClient();
		demo2_buildLiveClient();
		demo3_initiatePayment();
		demo4_singleLookup();
		demo5_pollWithDefaultIntervalStrategy();
		demo6_pollWithBackoffStrategy();
		demo7_pollWithSimpleStrategy();
		demo8_handlePollTimeout();
		demo9_refundPayment();
		demo10_handleApiErrors();
		
		System.out.println("\n=== All demos complete ===");
	}
	
	// ==================================================================
	// Demo 1 — Building a sandbox client
	// ==================================================================
	
	/**
	 * Shows how to construct an {@link EcocashClient} for the <em>sandbox</em>
	 * environment (the default when no mode is set).
	 *
	 * <p>The client implements {@link AutoCloseable} so it works cleanly inside
	 * a try-with-resources block — the recommended pattern for production code.</p>
	 */
	static void demo1_buildSandboxClient() {
		System.out.println("--- Demo 1: Build a sandbox client ---");
		
		// Fluent builder — only apiKey and merchantCode are required.
		// All other settings (timeouts, mode) have sensible defaults.
		try (EcocashClient client = EcocashClient.builder()
				                            .apiKey(API_KEY)
				                            .merchantCode(MERCHANT_CODE)
				                            // connectTimeoutSeconds defaults to 30 s
				                            // requestTimeoutSeconds defaults to 60 s
				                            .build()) {
			
			System.out.println("Mode    : " + client.getMode());      // "sandbox"
			System.out.println("Is live : " + client.isLiveMode());   // false
			
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 2 — Building a live client
	// ==================================================================
	
	/**
	 * Shows how to switch to the <em>live</em> environment and how to
	 * customise the underlying HTTP timeouts for production workloads.
	 */
	static void demo2_buildLiveClient() {
		System.out.println("--- Demo 2: Build a live client ---");
		
		try (EcocashClient client = EcocashClient.builder()
				                            .apiKey(API_KEY)
				                            .merchantCode(MERCHANT_CODE)
				                            .liveMode()                     // flips mode from "sandbox" → "live"
				                            .connectTimeoutSeconds(10)       // TCP connect timeout
				                            .requestTimeoutSeconds(120)      // per-request read timeout
				                            .build()) {
			
			System.out.println("Mode    : " + client.getMode());     // "live"
			System.out.println("Is live : " + client.isLiveMode());  // true
			
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 3 — Initiating a payment
	// ==================================================================
	
	/**
	 * Demonstrates {@link EcocashClient#initPayment(String, double, String)}.
	 *
	 * <p>The method generates a UUID reference internally, sends the C2B
	 * payment prompt to the customer's handset, and returns an
	 * {@link InitPaymentResponse} that acts as your receipt — keep it to
	 * poll for the transaction outcome.</p>
	 */
	static void demo3_initiatePayment() {
		System.out.println("--- Demo 3: Initiate a payment ---");
		
		try (EcocashClient client = buildSandboxClient()) {
			
			// initPayment(phone, amount, reason)
			//   phone  — customer MSISDN; must not be blank
			//   amount — positive USD amount
			//   reason — description shown to the customer on their handset
			InitPaymentResponse payment =
					client.initPayment(CUSTOMER_PHONE, 20.05, "bread");
			
			System.out.println("Payment initiated!");
			System.out.println("  Phone           : " + payment.getPhone());
			System.out.println("  Amount          : " + payment.getAmount() + " " + payment.getCurrency());
			System.out.println("  Reason          : " + payment.getReason());
			System.out.println("  Source reference: " + payment.getSourceReference()); // the UUID to poll with
			System.out.println("  Gateway status  : " + payment.getStatus());          // may be null initially
			
		} catch (EcocashApiException e) {
			// Non-2xx HTTP response from the Ecocash gateway
			System.err.println("Gateway error [HTTP " + e.getHttpStatus() + "]: " + e.getMessage());
		} catch (EcocashException e) {
			System.err.println("SDK error: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 4 — Single transaction lookup (no polling)
	// ==================================================================
	
	/**
	 * Shows how to perform a <em>single</em> status lookup without any retry
	 * logic, using {@link EcocashClient#lookupTransaction(String, String)}.
	 *
	 * <p>Use this when you manage the retry loop yourself — for example
	 * inside a scheduled job or a webhook-driven flow.</p>
	 */
	static void demo4_singleLookup() {
		System.out.println("--- Demo 4: Single transaction lookup ---");
		
		try (EcocashClient client = buildSandboxClient()) {
			
			// In a real scenario these come from a stored InitPaymentResponse
			String savedReference = "325a802f-943e-47c2-addf-010285f09cea";
			String savedPhone     = CUSTOMER_PHONE;
			
			LookupTransactionResponse tx =
					client.lookupTransaction(savedReference, savedPhone);
			
			System.out.println("Status            : " + tx.getStatus());
			System.out.println("Payment success   : " + tx.isPaymentSuccess());
			System.out.println("Ecocash reference : " + tx.getEcocashReference());
			System.out.println("Transaction time  : " + tx.getTransactionDateTime());
			
			if (tx.getAmount() != null) {
				System.out.println("Amount            : " + tx.getAmount().getAmount()
						                   + " " + tx.getAmount().getCurrency());
			}
			
			// Safe status comparison using the constant — avoids hardcoded strings
			if (LookupTransactionResponse.STATUS_PENDING_VALIDATION.equals(tx.getStatus())) {
				System.out.println("  → Customer has not yet approved the prompt.");
			}
			
		} catch (EcocashException e) {
			System.err.println("Lookup failed: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 5 — Polling with the default INTERVAL strategy
	// ==================================================================
	
	/**
	 * The simplest and most common polling pattern.
	 *
	 * <p>{@link EcocashClient#pollTransaction(InitPaymentResponse)} uses
	 * {@link PollStrategy#INTERVAL} with {@link PollOptions#defaults()}:
	 * 10 attempts, 1 second apart. A
	 * {@link PollTimeoutException} is thrown if the customer does not
	 * respond within that window.</p>
	 */
	static void demo5_pollWithDefaultIntervalStrategy() {
		System.out.println("--- Demo 5: Poll — default INTERVAL strategy ---");
		
		try (EcocashClient client = buildSandboxClient()) {
			
			InitPaymentResponse payment =
					client.initPayment(CUSTOMER_PHONE, 5.00, "coffee");
			
			// Single-argument overload — uses INTERVAL + PollOptions.defaults()
			LookupTransactionResponse result = client.pollTransaction(payment);
			
			if (result.isPaymentSuccess()) {
				System.out.println("Payment confirmed: " + result.getEcocashReference());
			} else {
				// Should not reach here normally — PollTimeoutException is thrown on exhaustion
				System.out.println("Unexpected status: " + result.getStatus());
			}
			
		} catch (PollTimeoutException e) {
			// Polling exhausted all attempts — inspect the last status we received
			LookupTransactionResponse last = e.getLastResponse();
			System.err.println("Timed out. Last status: "
					                   + (last != null ? last.getStatus() : "unknown"));
		} catch (EcocashException e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 6 — Polling with exponential BACKOFF strategy
	// ==================================================================
	
	/**
	 * Uses {@link PollStrategy#BACKOFF} to space out requests progressively,
	 * reducing load on the gateway when approval is slow.
	 *
	 * <p>Sleep schedule with the settings below:
	 * <pre>
	 *   Attempt 1 → wait 2 s
	 *   Attempt 2 → wait 4 s
	 *   Attempt 3 → wait 8 s
	 *   Attempt 4 → wait 16 s
	 *   Attempt 5 → wait 32 s   ← last attempt before timeout
	 * </pre>
	 * </p>
	 */
	static void demo6_pollWithBackoffStrategy() {
		System.out.println("--- Demo 6: Poll — BACKOFF strategy ---");
		
		// Build custom poll options via the fluent builder
		PollOptions options = PollOptions.builder()
				                      .sleepMs(2_000)     // initial sleep: 2 seconds
				                      .multiplier(2)      // double the wait after each attempt
				                      .maxAttempts(5)     // give up after 5 tries
				                      .build();
		
		try (EcocashClient client = buildSandboxClient()) {
			
			InitPaymentResponse payment =
					client.initPayment(CUSTOMER_PHONE, 15.00, "groceries");
			
			LookupTransactionResponse result =
					client.pollTransaction(payment, PollStrategy.BACKOFF, options);
			
			System.out.println("Paid: " + result.isPaymentSuccess()
					                   + " | ref: " + result.getEcocashReference());
			
		} catch (PollTimeoutException e) {
			LookupTransactionResponse last = e.getLastResponse();
			System.err.println("Backoff timed out after " + options.getMaxAttempts()
					                   + " attempts. Last status: " + (last != null ? last.getStatus() : "unknown"));
		} catch (EcocashException e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 7 — Polling with SIMPLE (tight-loop) strategy
	// ==================================================================
	
	/**
	 * {@link PollStrategy#SIMPLE} polls in a tight loop with no delay.
	 *
	 * <p><strong>Only use this in sandbox / test environments.</strong>
	 * Rapid polling against the live gateway may result in rate-limiting.</p>
	 */
	static void demo7_pollWithSimpleStrategy() {
		System.out.println("--- Demo 7: Poll — SIMPLE strategy (no delay) ---");
		
		// maxAttempts is still respected — we won't loop forever
		PollOptions options = PollOptions.builder()
				                      .maxAttempts(3)
				                      .build();
		
		try (EcocashClient client = buildSandboxClient()) {
			
			InitPaymentResponse payment =
					client.initPayment(CUSTOMER_PHONE, 1.00, "test purchase");
			
			LookupTransactionResponse result =
					client.pollTransaction(payment, PollStrategy.SIMPLE, options);
			
			System.out.println("Status: " + result.getStatus());
			
		} catch (PollTimeoutException e) {
			System.err.println("Simple poll timed out: " + e.getMessage());
		} catch (EcocashException e) {
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 8 — Handling a poll timeout gracefully
	// ==================================================================
	
	/**
	 * Shows how to recover when polling exhausts all attempts, e.g. the
	 * customer ignored the payment prompt.
	 *
	 * <p>The {@link PollTimeoutException} always carries the last
	 * {@link LookupTransactionResponse} so you can log the reference and
	 * decide on a retry or cancellation strategy.</p>
	 */
	static void demo8_handlePollTimeout() {
		System.out.println("--- Demo 8: Graceful poll timeout handling ---");
		
		// Very short options to force a quick timeout in the demo
		PollOptions tightOptions = PollOptions.builder()
				                           .maxAttempts(2)
				                           .sleepMs(500)
				                           .build();
		
		try (EcocashClient client = buildSandboxClient()) {
			
			InitPaymentResponse payment =
					client.initPayment(CUSTOMER_PHONE, 50.00, "electronics");
			
			client.pollTransaction(payment, PollStrategy.INTERVAL, tightOptions);
			
			// If we get here, the customer paid within 2 attempts — great!
			System.out.println("Payment completed before timeout.");
			
		} catch (PollTimeoutException e) {
			// Customer did not respond in time — log, notify, or schedule a retry
			LookupTransactionResponse last = e.getLastResponse();
			
			System.err.println("Customer did not complete payment in time.");
			if (last != null) {
				System.err.println("  Last status    : " + last.getStatus());
				System.err.println("  Last reference : " + last.getReference());
			}
			
			// Example: persist the reference for a background retry job
			scheduleRetry(last);
			
		} catch (EcocashException e) {
			System.err.println("Unexpected error during polling: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 9 — Refunding a payment
	// ==================================================================
	
	/**
	 * Demonstrates the full refund flow using {@link RefundDetails} built
	 * with its fluent builder, and inspecting {@link RefundResponse}.
	 *
	 * <p>The Ecocash reference ({@code ecocashReference}) from the original
	 * lookup response is used as the {@code reference} parameter here.</p>
	 */
	static void demo9_refundPayment() {
		System.out.println("--- Demo 9: Refund a payment ---");
		
		// Build the refund request — all fields are validated in build()
		RefundDetails details = RefundDetails.builder()
				                        .reference("MP250908.1537.A22242")  // original Ecocash transaction reference
				                        .phone(CUSTOMER_PHONE)
				                        .amount(20.05)
				                        .clientName("Acme Grocery Store")
				                        .reason("The bread was stale — customer complaint #1234")
				                        // .currency("USD") — optional, defaults to USD
				                        .build();
		
		try (EcocashClient client = buildSandboxClient()) {
			
			RefundResponse refund = client.refundPayment(details);
			
			System.out.println("Refund status      : " + refund.getTransactionStatus());
			System.out.println("Completed          : " + refund.isCompleted());
			System.out.println("Ecocash ref        : " + refund.getEcocashReference());
			System.out.println("Amount             : " + refund.getAmount() + " " + refund.getCurrency());
			System.out.println("Transaction start  : " + refund.getTransactionStartTime());
			System.out.println("Transaction end    : " + refund.getTransactionEndTime());
			
			if (refund.isCompleted()) {
				System.out.println("  → Refund processed successfully.");
			} else {
				System.out.println("  → Refund status: " + refund.getResponseMessage());
			}
			
		} catch (EcocashApiException e) {
			System.err.println("Gateway rejected refund [HTTP " + e.getHttpStatus() + "]: "
					                   + e.getResponseBody());
		} catch (EcocashException e) {
			System.err.println("Refund failed: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Demo 10 — Handling API errors
	// ==================================================================
	
	/**
	 * Illustrates the full exception hierarchy and how to handle each layer.
	 *
	 * <p>Best practice is to catch the most specific exception first:</p>
	 * <ol>
	 *   <li>{@link PollTimeoutException} — polling ran out of attempts</li>
	 *   <li>{@link EcocashApiException}  — gateway returned non-2xx HTTP</li>
	 *   <li>{@link EcocashException}     — any other SDK / I/O error</li>
	 * </ol>
	 */
	static void demo10_handleApiErrors() {
		System.out.println("--- Demo 10: Error handling ---");
		
		// Intentionally bad credentials to trigger an API error
		try (EcocashClient badClient = EcocashClient.builder()
				                               .apiKey("invalid-key")
				                               .merchantCode("invalid-merchant")
				                               .build()) {
			
			badClient.initPayment(CUSTOMER_PHONE, 10.00, "test");
			
		} catch (PollTimeoutException e) {
			// Only relevant when calling pollTransaction — won't fire here,
			// but included to show the correct catch order.
			System.err.println("Poll timed out: " + e.getMessage());
			
		} catch (EcocashApiException e) {
			// Gateway returned 4xx or 5xx — most likely here with bad credentials
			System.err.println("API error caught!");
			System.err.println("  HTTP status   : " + e.getHttpStatus());
			System.err.println("  Response body : " + e.getResponseBody());
			// Decide: 401 → bad key, 429 → rate limited, 5xx → gateway down
			
		} catch (EcocashException e) {
			// Network timeout, parse failure, interrupted thread, etc.
			System.err.println("SDK error: " + e.getMessage());
			if (e.getCause() != null) {
				System.err.println("  Caused by: " + e.getCause().getClass().getSimpleName());
			}
		}
		
		// ---------------------------------------------------------------
		// Showing how IllegalArgumentException guards bad input BEFORE
		// any network call is made
		// ---------------------------------------------------------------
		try (EcocashClient client = buildSandboxClient()) {
			
			// amount <= 0 triggers immediate validation failure
			client.initPayment(CUSTOMER_PHONE, -5.00, "bad amount");
			
		} catch (IllegalArgumentException e) {
			// Thrown synchronously — no network was called
			System.err.println("Input validation caught: " + e.getMessage());
		} catch (EcocashException e) {
			System.err.println("Unexpected: " + e.getMessage());
		}
		
		System.out.println();
	}
	
	// ==================================================================
	// Private helpers used across demos
	// ==================================================================
	
	/**
	 * Convenience factory that creates a sandbox {@link EcocashClient} with
	 * default timeouts. Centralised here so all demos stay readable.
	 *
	 * @return a new sandbox client; caller must close it
	 */
	private static EcocashClient buildSandboxClient() {
		return EcocashClient.builder()
				       .apiKey(API_KEY)
				       .merchantCode(MERCHANT_CODE)
				       .build();
	}
	
	/**
	 * Placeholder for a real retry-scheduling mechanism.
	 *
	 * <p>In a production system this might persist the reference to a database,
	 * push a message onto a queue, or set a timer for a follow-up check.</p>
	 *
	 * @param lastResponse the last response received during polling, may be {@code null}
	 */
	private static void scheduleRetry(LookupTransactionResponse lastResponse) {
		if (lastResponse == null) {
			System.out.println("  [scheduleRetry] No response data — cannot schedule retry.");
			return;
		}
		System.out.println("  [scheduleRetry] Queuing retry for reference: "
				                   + lastResponse.getReference());
		// e.g. retryQueue.add(new RetryTask(lastResponse.getReference(), Instant.now().plusSeconds(300)));
	}
}
