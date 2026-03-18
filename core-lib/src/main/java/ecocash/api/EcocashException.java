package ecocash.api;

/**
 * Base exception for all Ecocash SDK errors.
 *
 * <p>All checked and unchecked errors thrown by this library extend this class,
 * so callers can catch the single type {@code EcocashException} to handle any
 * SDK-level failure.</p>
 *
 * <pre>{@code
 * try {
 *     InitPaymentResponse payment = client.initPayment("26377000000", 20.05, "bread");
 * } catch (EcocashApiException e) {
 *     // HTTP-level or API-level error from the Ecocash gateway
 *     System.err.println("HTTP " + e.getHttpStatus() + ": " + e.getMessage());
 * } catch (EcocashException e) {
 *     // Any other SDK error (serialisation, polling timeout, etc.)
 *     System.err.println("SDK error: " + e.getMessage());
 * }
 * }</pre>
 */
public class EcocashException extends Exception {
	
	public EcocashException(String message) {
		super(message);
	}
	
	public EcocashException(String message, Throwable cause) {
		super(message, cause);
	}
	
	// ------------------------------------------------------------------
	// Specialised sub-types
	// ------------------------------------------------------------------
	
	/**
	 * Thrown when the Ecocash gateway returns a non-2xx HTTP status code.
	 */
	public static final class EcocashApiException extends EcocashException {
		
		private final int httpStatus;
		private final String responseBody;
		
		EcocashApiException(int httpStatus, String responseBody) {
			super("Ecocash API error [HTTP " + httpStatus + "]: " + responseBody);
			this.httpStatus = httpStatus;
			this.responseBody = responseBody;
		}
		
		/** The HTTP status code returned by the gateway. */
		public int getHttpStatus() { return httpStatus; }
		
		/** The raw response body returned by the gateway. */
		public String getResponseBody() { return responseBody; }
	}
	
	/**
	 * Thrown when polling exhausts all configured attempts without a terminal
	 * transaction status.
	 */
	public static final class PollTimeoutException extends EcocashException {
		
		private final LookupTransactionResponse lastResponse;
		
		PollTimeoutException(LookupTransactionResponse lastResponse) {
			super("Polling timed out. Last known status: "
					      + (lastResponse != null ? lastResponse.getStatus() : "unknown"));
			this.lastResponse = lastResponse;
		}
		
		/**
		 * The last {@link LookupTransactionResponse} received before the
		 * timeout, or {@code null} if no lookup ever succeeded.
		 */
		public LookupTransactionResponse getLastResponse() { return lastResponse; }
	}
}