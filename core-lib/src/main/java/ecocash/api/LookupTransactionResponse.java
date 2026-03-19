package ecocash.api;

/**
 * Represents the result of a transaction status lookup or completed poll.
 *
 * <p>Check {@link #isPaymentSuccess()} as a convenient boolean shortcut; for
 * richer logic inspect {@link #getStatus()} directly against the known values
 * documented in the Ecocash developer portal.</p>
 *
 * <pre>{@code
 * LookupTransactionResponse tx = client.lookupTransaction(reference, phone);
 * switch (tx.getStatus()) {
 *     case "SUCCESS":           handleSuccess(tx);  break;
 *     case "PENDING_VALIDATION": waitAndRetry();    break;
 *     default:                  handleUnknown(tx); break;
 * }
 * }</pre>
 *
 * <strong>Known status values</strong>
 * <ul>
 *   <li>{@code SUCCESS} – payment was completed successfully.</li>
 *   <li>{@code PENDING_VALIDATION} – the user has not yet approved the prompt.</li>
 * </ul>
 */
public final class LookupTransactionResponse {
	
	/** Known terminal / informational status strings returned by the gateway. */
	public static final String STATUS_SUCCESS            = "SUCCESS";
	public static final String STATUS_PENDING_VALIDATION = "PENDING_VALIDATION";
	
	// ------------------------------------------------------------------
	// Nested value type
	// ------------------------------------------------------------------
	
	/**
	 * Money representation embedded in the lookup response.
	 */
	public static final class TransactionAmount {
		private final double amount;
		private final String currency;
		
		TransactionAmount(double amount, String currency) {
			this.amount = amount;
			this.currency = currency;
		}
		
		/** Numeric amount. */
		public double getAmount() { return amount; }
		
		/** ISO-4217 currency code, e.g. {@code "USD"}. */
		public String getCurrency() { return currency; }
		
		@Override
		public String toString() {
			return amount + " " + currency;
		}
	}
	
	// ------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------
	
	private final TransactionAmount amount;
	private final String customerMsisdn;
	private final String reference;
	private final String ecocashReference;
	private final boolean paymentSuccess;
	private final String status;
	private final String transactionDateTime;
	
	/** Package-private — constructed only by {@link EcocashClient}. */
	LookupTransactionResponse(
			TransactionAmount amount,
			String customerMsisdn,
			String reference,
			String ecocashReference,
			String status,
			String transactionDateTime) {
		this.amount = amount;
		this.customerMsisdn = customerMsisdn;
		this.reference = reference;
		this.ecocashReference = ecocashReference;
		this.status = status;
		this.transactionDateTime = transactionDateTime;
		this.paymentSuccess = STATUS_SUCCESS.equals(status);
	}
	
	// ------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------
	
	/**
	 * The transaction amount and currency as reported by the gateway,
	 * or {@code null} if the gateway did not return this field.
	 */
	public TransactionAmount getAmount() { return amount; }
	
	/** Customer MSISDN associated with the transaction. */
	public String getCustomerMsisdn() { return customerMsisdn; }
	
	/** Source reference (UUID) that was provided at payment initiation. */
	public String getReference() { return reference; }
	
	/** Ecocash-internal reference (e.g. {@code "MP250908.1537.A22242"}). */
	public String getEcocashReference() { return ecocashReference; }
	
	/**
	 * {@code true} if and only if {@link #getStatus()} equals
	 * {@link #STATUS_SUCCESS}.
	 */
	public boolean isPaymentSuccess() { return paymentSuccess; }
	
	/**
	 * Raw status string from the gateway. Compare against the
	 * {@code STATUS_*} constants on this class for safe equality checks.
	 */
	public String getStatus() { return status; }
	
	/** ISO-like datetime string from the gateway, e.g. {@code "2025-09-08 15:37:16"}. */
	public String getTransactionDateTime() { return transactionDateTime; }
	
	@Override
	public String toString() {
		return "LookupTransactionResponse{"
				       + "status='" + status + '\''
				       + ", paymentSuccess=" + paymentSuccess
				       + ", reference='" + reference + '\''
				       + ", ecocashReference='" + ecocashReference + '\''
				       + ", amount=" + amount
				       + ", transactionDateTime='" + transactionDateTime + '\''
				       + '}';
	}
}