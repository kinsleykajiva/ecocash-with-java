package ecocash.api;

/**
 * Represents the result of a successful {@code initPayment} call.
 *
 * <p>This object acts as a receipt for the payment initiation. Pass it directly
 * to {@link EcocashClient#pollTransaction(InitPaymentResponse)} or
 * {@link EcocashClient#lookupTransaction(String, String)} to check the outcome.</p>
 *
 * <pre>{@code
 * InitPaymentResponse payment = client.initPayment("26377000000", 20.05, "bread");
 * System.out.println("Reference: " + payment.getSourceReference());
 *
 * LookupTransactionResponse status = client.pollTransaction(payment);
 * if (status.isPaymentSuccess()) {
 *     System.out.println("Paid!");
 * }
 * }</pre>
 */
public final class InitPaymentResponse {
	
	private final String phone;
	private final double amount;
	private final String reason;
	private final String currency;
	private final String sourceReference;
	
	// Fields merged from the raw API response body (may be null)
	private final String status;
	private final String transactionStatus;
	private final String ecocashReference;
	
	/** Package-private — constructed only by {@link EcocashClient}. */
	InitPaymentResponse(
			String phone,
			double amount,
			String reason,
			String currency,
			String sourceReference,
			String status,
			String transactionStatus,
			String ecocashReference) {
		this.phone = phone;
		this.amount = amount;
		this.reason = reason;
		this.currency = currency;
		this.sourceReference = sourceReference;
		this.status = status;
		this.transactionStatus = transactionStatus;
		this.ecocashReference = ecocashReference;
	}
	
	// ------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------
	
	/** The customer's MSISDN supplied at initiation (e.g. {@code "26377000000"}). */
	public String getPhone() { return phone; }
	
	/** The payment amount in the given currency. */
	public double getAmount() { return amount; }
	
	/** Human-readable reason / description for the payment. */
	public String getReason() { return reason; }
	
	/** Currency code, typically {@code "USD"}. */
	public String getCurrency() { return currency; }
	
	/**
	 * The UUID reference generated client-side and echoed by the gateway.
	 * Use this value to poll for the transaction status.
	 */
	public String getSourceReference() { return sourceReference; }
	
	/**
	 * Top-level {@code status} field returned by the gateway, or {@code null}
	 * if the gateway did not include it in the response.
	 */
	public String getStatus() { return status; }
	
	/**
	 * {@code transactionStatus} field returned by the gateway, or {@code null}.
	 */
	public String getTransactionStatus() { return transactionStatus; }
	
	/**
	 * The Ecocash-internal transaction reference, or {@code null} if the
	 * payment has not yet been assigned one.
	 */
	public String getEcocashReference() { return ecocashReference; }
	
	@Override
	public String toString() {
		return "InitPaymentResponse{"
				       + "phone='" + phone + '\''
				       + ", amount=" + amount
				       + ", reason='" + reason + '\''
				       + ", currency='" + currency + '\''
				       + ", sourceReference='" + sourceReference + '\''
				       + ", status='" + status + '\''
				       + '}';
	}
}