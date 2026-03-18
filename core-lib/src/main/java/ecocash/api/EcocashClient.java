package ecocash.api;

import org.json.JSONObject;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the Ecocash Open API Java SDK.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * // 1. Build the client (sandbox mode by default)
 * EcocashClient client = EcocashClient.builder()
 *         .apiKey("your-api-key")
 *         .merchantCode("your-merchant-code")
 *         .build();
 *
 * // 2. Initiate a payment
 * InitPaymentResponse payment = client.initPayment("26377000000", 20.05, "bread");
 *
 * // 3. Poll until SUCCESS or timeout
 * LookupTransactionResponse result = client.pollTransaction(payment);
 * if (result.isPaymentSuccess()) {
 *     System.out.println("Payment confirmed: " + result.getEcocashReference());
 * }
 *
 * // 4. Close the underlying HTTP client when done
 * client.close();
 * }</pre>
 *
 * <h2>Going live</h2>
 * <pre>{@code
 * EcocashClient client = EcocashClient.builder()
 *         .apiKey("your-api-key")
 *         .merchantCode("your-merchant-code")
 *         .liveMode()
 *         .build();
 * }</pre>
 *
 * <h2>Custom HTTP timeouts</h2>
 * <pre>{@code
 * EcocashClient client = EcocashClient.builder()
 *         .apiKey("key")
 *         .merchantCode("merchant")
 *         .connectTimeoutSeconds(15)
 *         .requestTimeoutSeconds(90)
 *         .build();
 * }</pre>
 *
 * <h2>Polling strategies</h2>
 * <pre>{@code
 * // Exponential back-off: 2 s → 4 s → 8 s …, up to 6 attempts
 * PollOptions opts = PollOptions.builder()
 *         .sleepMs(2_000)
 *         .multiplier(2)
 *         .maxAttempts(6)
 *         .build();
 *
 * LookupTransactionResponse tx =
 *         client.pollTransaction(payment, PollStrategy.BACKOFF, opts);
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> instances of this class are thread-safe
 * after construction. The underlying {@link java.net.http.HttpClient} is
 * shared across all calls.</p>
 *
 * <p><strong>Dependencies:</strong> requires {@code org.json:json} on the classpath:</p>
 * <pre>
 * Maven:  &lt;dependency&gt;&lt;groupId&gt;org.json&lt;/groupId&gt;
 *                     &lt;artifactId&gt;json&lt;/artifactId&gt;
 *                     &lt;version&gt;20240303&lt;/version&gt;&lt;/dependency&gt;
 * Gradle: implementation 'org.json:json:20240303'
 * </pre>
 */
public final class EcocashClient implements Closeable {
	
	private static final Logger LOG = Logger.getLogger(EcocashClient.class.getName());
	
	// ------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------
	
	private static final String BASE_URL = "https://developers.ecocash.co.zw/api/ecocash_pay";
	private static final String MODE_SANDBOX = "sandbox";
	private static final String MODE_LIVE = "live";
	
	// ------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------
	
	private final String apiKey;
	private final String merchantCode;
	private final String mode;
	private final Duration requestTimeout;
	private final HttpClient httpClient;
	
	// ------------------------------------------------------------------
	// Private constructor — use the Builder
	// ------------------------------------------------------------------
	
	private EcocashClient(Builder b) {
		this.apiKey = b.apiKey;
		this.merchantCode = b.merchantCode;
		this.mode = b.live ? MODE_LIVE : MODE_SANDBOX;
		this.requestTimeout = Duration.ofSeconds(b.requestTimeoutSeconds);
		
		this.httpClient = HttpClient.newBuilder()
				                  .connectTimeout(Duration.ofSeconds(b.connectTimeoutSeconds))
				                  .build();
	}
	
	// ------------------------------------------------------------------
	// Factory / Builder
	// ------------------------------------------------------------------
	
	/**
	 * Returns a new {@link Builder} for {@link EcocashClient}.
	 *
	 * @return a fresh builder
	 */
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * Fluent builder for {@link EcocashClient}.
	 *
	 * <p>Only {@link #apiKey(String)} and {@link #merchantCode(String)} are
	 * required; all other settings have sensible defaults.</p>
	 */
	public static final class Builder {
		
		private String apiKey;
		private String merchantCode;
		private boolean live = false;
		private int connectTimeoutSeconds = 30;
		private int requestTimeoutSeconds = 60;
		
		private Builder() {
		}
		
		/**
		 * Sets the Ecocash API key (required).
		 *
		 * @param apiKey non-null API key
		 * @return this builder
		 */
		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}
		
		/**
		 * Sets the merchant code registered with Ecocash (required).
		 *
		 * @param merchantCode non-null merchant code
		 * @return this builder
		 */
		public Builder merchantCode(String merchantCode) {
			this.merchantCode = merchantCode;
			return this;
		}
		
		/**
		 * Switches the client to <strong>live</strong> mode.
		 * Default is sandbox mode.
		 *
		 * @return this builder
		 */
		public Builder liveMode() {
			this.live = true;
			return this;
		}
		
		/**
		 * Sets the TCP connect timeout in seconds (default: 30).
		 *
		 * @param seconds positive integer
		 * @return this builder
		 */
		public Builder connectTimeoutSeconds(int seconds) {
			this.connectTimeoutSeconds = seconds;
			return this;
		}
		
		/**
		 * Sets the per-request read timeout in seconds (default: 60).
		 *
		 * @param seconds positive integer
		 * @return this builder
		 */
		public Builder requestTimeoutSeconds(int seconds) {
			this.requestTimeoutSeconds = seconds;
			return this;
		}
		
		/**
		 * Builds an {@link EcocashClient}.
		 *
		 * @return configured client
		 *
		 * @throws NullPointerException if {@code apiKey} or {@code merchantCode} is null
		 */
		public EcocashClient build() {
			Objects.requireNonNull(apiKey, "apiKey is required");
			Objects.requireNonNull(merchantCode, "merchantCode is required");
			return new EcocashClient(this);
		}
	}
	
	// ------------------------------------------------------------------
	// Public API — configuration queries
	// ------------------------------------------------------------------
	
	/**
	 * Returns the active mode string: {@code "sandbox"} or {@code "live"}.
	 *
	 * @return current mode
	 */
	public String getMode() {
		return mode;
	}
	
	/**
	 * Returns {@code true} if this client is configured for live mode.
	 *
	 * @return {@code true} when live
	 */
	public boolean isLiveMode() {
		return MODE_LIVE.equals(mode);
	}
	
	// ------------------------------------------------------------------
	// Public API — payments
	// ------------------------------------------------------------------
	
	/**
	 * Initiates a customer-to-business (C2B) payment request.
	 *
	 * <p>The customer will receive a payment prompt on their handset.
	 * Use the returned {@link InitPaymentResponse} with
	 * {@link #pollTransaction(InitPaymentResponse)} or
	 * {@link #lookupTransaction(String, String)} to check the outcome.</p>
	 *
	 * @param phone  customer MSISDN e.g. {@code "26377000000"}
	 * @param amount positive payment amount in USD
	 * @param reason human-readable payment description shown to the customer
	 * @return payment initiation receipt
	 *
	 * @throws IllegalArgumentException             if {@code phone} is blank, {@code amount} is
	 *                                              not positive, or {@code reason} is blank
	 * @throws EcocashException.EcocashApiException if the gateway returns a non-2xx status
	 * @throws EcocashException                     on any other SDK or I/O error
	 */
	public InitPaymentResponse initPayment(String phone, double amount, String reason)
			throws EcocashException {
		validatePhone(phone);
		validateAmount(amount);
		validateReason(reason);
		
		String reference = UUID.randomUUID().toString();
		String url = BASE_URL + "/api/v2/payment/instant/c2b/" + mode;
		
		JSONObject body = new JSONObject();
		body.put("customerMsisdn", phone);
		body.put("phone", phone);
		body.put("amount", amount);
		body.put("reason", reason);
		body.put("currency", "USD");
		body.put("sourceReference", reference);
		
		LOG.log(Level.FINE, "initPayment -> phone={0}, amount={1}, reference={2}",
				new Object[]{phone, amount, reference});
		
		JSONObject raw = post(url, body);
		
		return new InitPaymentResponse(
				phone,
				amount,
				reason,
				"USD",
				reference,
				raw.optString("status", null),
				raw.optString("transactionStatus", null),
				raw.optString("ecocashReference", null));
	}
	
	// ------------------------------------------------------------------
	// Public API — refunds
	// ------------------------------------------------------------------
	
	/**
	 * Requests a refund for a previously completed transaction.
	 *
	 * <pre>{@code
	 * RefundDetails details = RefundDetails.builder()
	 *         .reference("MP250908.1537.A22242")
	 *         .phone("26377000000")
	 *         .amount(20.05)
	 *         .clientName("Acme Store")
	 *         .reason("Defective product")
	 *         .build();
	 *
	 * RefundResponse refund = client.refundPayment(details);
	 * System.out.println(refund.isCompleted()); // true if processed
	 * }</pre>
	 *
	 * @param details non-null refund parameters
	 * @return refund result from the gateway
	 *
	 * @throws NullPointerException                 if {@code details} is null
	 * @throws EcocashException.EcocashApiException if the gateway returns a non-2xx status
	 * @throws EcocashException                     on any other SDK or I/O error
	 */
	public RefundResponse refundPayment(RefundDetails details) throws EcocashException {
		Objects.requireNonNull(details, "details must not be null");
		
		String url = BASE_URL + "/api/v2/refund/instant/c2b/" + mode;
		
		JSONObject body = new JSONObject();
		body.put("origionalEcocashTransactionReference", details.getReference());
		body.put("refundCorelator", "012345l61975");
		body.put("sourceMobileNumber", details.getPhone());
		body.put("amount", details.getAmount());
		body.put("clientName", details.getClientName());
		body.put("currency", details.getCurrency());
		body.put("reasonForRefund", details.getReason());
		
		LOG.log(Level.FINE, "refundPayment -> reference={0}, amount={1}",
				new Object[]{details.getReference(), details.getAmount()});
		
		JSONObject raw = post(url, body);
		return mapRefundResponse(raw);
	}
	
	// ------------------------------------------------------------------
	// Public API — lookup
	// ------------------------------------------------------------------
	
	/**
	 * Performs a single transaction status lookup.
	 *
	 * <p>For automatic retry logic prefer {@link #pollTransaction(InitPaymentResponse)}.</p>
	 *
	 * @param sourceReference the UUID reference from {@link InitPaymentResponse#getSourceReference()}
	 * @param phone           the customer MSISDN used at payment initiation
	 * @return current transaction status
	 *
	 * @throws EcocashException.EcocashApiException if the gateway returns a non-2xx status
	 * @throws EcocashException                     on any other SDK or I/O error
	 */
	public LookupTransactionResponse lookupTransaction(String sourceReference, String phone)
			throws EcocashException {
		Objects.requireNonNull(sourceReference, "sourceReference must not be null");
		Objects.requireNonNull(phone, "phone must not be null");
		
		String url = BASE_URL + "/api/v1/transaction/c2b/status/" + mode;
		
		JSONObject body = new JSONObject();
		body.put("sourceMobileNumber", phone);
		body.put("sourceReference", sourceReference);
		
		JSONObject raw = post(url, body);
		return mapLookupResponse(raw);
	}
	
	// ------------------------------------------------------------------
	// Public API — polling
	// ------------------------------------------------------------------
	
	/**
	 * Polls for transaction status using the {@link PollStrategy#INTERVAL INTERVAL}
	 * strategy with {@link PollOptions#defaults() default options}.
	 *
	 * <p>This is the simplest overload and the recommended starting point:</p>
	 * <pre>{@code
	 * LookupTransactionResponse result = client.pollTransaction(payment);
	 * if (result.isPaymentSuccess()) { ... }
	 * }</pre>
	 *
	 * @param initResponse the payment receipt returned by {@link #initPayment}
	 * @return the last {@link LookupTransactionResponse} received
	 *
	 * @throws EcocashException.PollTimeoutException if polling exhausts all attempts
	 * @throws EcocashException                      on I/O or API errors
	 */
	public LookupTransactionResponse pollTransaction(InitPaymentResponse initResponse)
			throws EcocashException {
		return pollTransaction(initResponse, PollStrategy.INTERVAL, PollOptions.defaults());
	}
	
	/**
	 * Polls for transaction status using the specified {@link PollStrategy} and
	 * {@link PollOptions}.
	 *
	 * <p>Polling stops as soon as a terminal status is reached ({@code SUCCESS})
	 * or {@link PollOptions#getMaxAttempts()} is exhausted, in which case a
	 * {@link EcocashException.PollTimeoutException} is thrown.</p>
	 *
	 * @param initResponse the payment receipt returned by {@link #initPayment}
	 * @param strategy     polling delay strategy; defaults to {@link PollStrategy#INTERVAL}
	 *                     when {@code null}
	 * @param options      polling configuration; defaults to {@link PollOptions#defaults()}
	 *                     when {@code null}
	 * @return the terminal {@link LookupTransactionResponse}
	 *
	 * @throws EcocashException.PollTimeoutException if polling exhausts all attempts
	 * @throws EcocashException                      on I/O or API errors
	 */
	public LookupTransactionResponse pollTransaction(
			InitPaymentResponse initResponse,
			PollStrategy strategy,
			PollOptions options) throws EcocashException {
		
		Objects.requireNonNull(initResponse, "initResponse must not be null");
		
		final PollStrategy effectiveStrategy = strategy != null ? strategy : PollStrategy.INTERVAL;
		final PollOptions effectiveOptions = options != null ? options : PollOptions.defaults();
		
		LOG.log(Level.FINE, "pollTransaction [strategy={0}, options={1}]",
				new Object[]{effectiveStrategy, effectiveOptions});
		
		LookupTransactionResponse lastResult = null;
		long sleepMs = effectiveOptions.getSleepMs();
		
		for (int attempt = 1 ; attempt <= effectiveOptions.getMaxAttempts() ; attempt++) {
			lastResult = lookupTransaction(
					initResponse.getSourceReference(),
					initResponse.getPhone());
			
			LOG.log(Level.FINE, "Poll attempt {0}/{1}: status={2}",
					new Object[]{attempt, effectiveOptions.getMaxAttempts(), lastResult.getStatus()});
			
			if (lastResult.isPaymentSuccess()) {
				return lastResult;
			}
			
			// Apply delay according to strategy
			switch (effectiveStrategy) {
				case INTERVAL:
					sleep(sleepMs);
					break;
				case BACKOFF:
					sleep(sleepMs);
					sleepMs *= effectiveOptions.getMultiplier();
					break;
				case SIMPLE:
					// no delay — intentional tight loop
					break;
				default:
					sleep(sleepMs);
					break;
			}
		}
		
		throw new EcocashException.PollTimeoutException(lastResult);
	}
	
	// ------------------------------------------------------------------
	// Closeable
	// ------------------------------------------------------------------
	
	/**
	 * Releases resources held by the underlying HTTP client.
	 *
	 * <p>After calling this method the client must not be reused.</p>
	 */
	@Override
	public void close() {
		// HttpClient does not implement Closeable in Java 11 but its executor
		// (if custom) should be shut down here.  The default shared executor
		// is managed by the JVM and requires no explicit shutdown.
		LOG.fine("EcocashClient closed.");
	}
	
	// ------------------------------------------------------------------
	// Private helpers — HTTP
	// ------------------------------------------------------------------
	
	/**
	 * Executes a POST request and returns the parsed JSON response body.
	 *
	 * @param url  fully-qualified endpoint URL
	 * @param body request payload
	 * @return parsed JSON object
	 *
	 * @throws EcocashException on any I/O, HTTP, or parse error
	 */
	private JSONObject post(String url, JSONObject body) throws EcocashException {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					                      .uri(URI.create(url))
					                      .header("Content-Type", "application/json")
					                      .header("X-API-KEY", apiKey)
					                      .header("Merchant", merchantCode)
					                      .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
					                      .timeout(requestTimeout)
					                      .build();
			
			HttpResponse<String> response =
					httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			int statusCode = response.statusCode();
			String responseBody = response.body();
			
			LOG.log(Level.FINE, "HTTP {0} <- {1}", new Object[]{statusCode, url});
			
			if (statusCode < 200 || statusCode >= 300) {
				throw new EcocashException.EcocashApiException(statusCode, responseBody);
			}
			
			return new JSONObject(responseBody);
			
		} catch (EcocashException e) {
			throw e; // re-throw our own exceptions
		} catch (Exception e) {
			throw new EcocashException("Unexpected error calling Ecocash API: " + e.getMessage(), e);
		}
	}
	
	// ------------------------------------------------------------------
	// Private helpers — sleep
	// ------------------------------------------------------------------
	
	private static void sleep(long ms) throws EcocashException {
		if (ms <= 0) return;
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EcocashException("Polling interrupted", e);
		}
	}
	
	// ------------------------------------------------------------------
	// Private helpers — JSON mappers
	// ------------------------------------------------------------------
	
	private static LookupTransactionResponse mapLookupResponse(JSONObject j) {
		LookupTransactionResponse.TransactionAmount amount = null;
		
		if (j.has("amount")) {
			JSONObject amt = j.getJSONObject("amount");
			amount = new LookupTransactionResponse.TransactionAmount(
					amt.optDouble("amount", 0),
					amt.optString("currency", "USD"));
		}
		
		return new LookupTransactionResponse(
				amount,
				j.optString("customerMsisdn", null),
				j.optString("reference", null),
				j.optString("ecocashReference", null),
				j.optString("status", null),
				j.optString("transactionDateTime", null));
	}
	
	private static RefundResponse mapRefundResponse(JSONObject j) {
		return new RefundResponse(
				j.optString("sourceReference", null),
				j.optString("transactionEndTime", null),
				j.optString("callbackUrl", null),
				j.optString("destinationReferenceCode", null),
				j.optString("sourceMobileNumber", null),
				j.optString("transactionStatus", null),
				j.optDouble("amount", 0),
				j.optString("destinationEcocashReference", null),
				j.optString("clientMerchantCode", null),
				j.optString("clientMerchantNumber", null),
				j.optString("clienttransactionDate", null),
				j.optString("description", null),
				j.optString("responseMessage", null),
				j.optString("currency", "USD"),
				j.optDouble("paymentAmount", 0),
				j.optString("ecocashReference", null),
				j.optString("transactionstartTime", null));
	}
	
	// ------------------------------------------------------------------
	// Private helpers — input validation
	// ------------------------------------------------------------------
	
	private static void validatePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			throw new IllegalArgumentException("phone must not be blank");
		}
	}
	
	private static void validateAmount(double amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("amount must be positive, got: " + amount);
		}
	}
	
	private static void validateReason(String reason) {
		if (reason == null || reason.isBlank()) {
			throw new IllegalArgumentException("reason must not be blank");
		}
	}
}