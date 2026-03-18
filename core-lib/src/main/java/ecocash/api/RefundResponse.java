package ecocash.api;

/**
 * Represents the gateway's response to a refund request.
 *
 * <p>Check {@link #isCompleted()} for a quick boolean result, or inspect
 * {@link #getTransactionStatus()} for the raw status string.</p>
 *
 * <pre>{@code
 * RefundResponse refund = client.refundPayment(details);
 * if (refund.isCompleted()) {
 *     System.out.println("Refunded via " + refund.getEcocashReference());
 * }
 * }</pre>
 */
public final class RefundResponse {
	
	/** Terminal status string indicating a successful refund. */
	public static final String STATUS_COMPLETED = "COMPLETED";
	
	// ------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------
	
	private final String sourceReference;
	private final String transactionEndTime;
	private final String callbackUrl;
	private final String destinationReferenceCode;
	private final String sourceMobileNumber;
	private final String transactionStatus;
	private final double amount;
	private final String destinationEcocashReference;
	private final String clientMerchantCode;
	private final String clientMerchantNumber;
	private final String clientTransactionDate;
	private final String description;
	private final String responseMessage;
	private final String currency;
	private final double paymentAmount;
	private final String ecocashReference;
	private final String transactionStartTime;
	
	/** Package-private — constructed only by {@link EcocashClient}. */
	RefundResponse(
			String sourceReference,
			String transactionEndTime,
			String callbackUrl,
			String destinationReferenceCode,
			String sourceMobileNumber,
			String transactionStatus,
			double amount,
			String destinationEcocashReference,
			String clientMerchantCode,
			String clientMerchantNumber,
			String clientTransactionDate,
			String description,
			String responseMessage,
			String currency,
			double paymentAmount,
			String ecocashReference,
			String transactionStartTime) {
		this.sourceReference = sourceReference;
		this.transactionEndTime = transactionEndTime;
		this.callbackUrl = callbackUrl;
		this.destinationReferenceCode = destinationReferenceCode;
		this.sourceMobileNumber = sourceMobileNumber;
		this.transactionStatus = transactionStatus;
		this.amount = amount;
		this.destinationEcocashReference = destinationEcocashReference;
		this.clientMerchantCode = clientMerchantCode;
		this.clientMerchantNumber = clientMerchantNumber;
		this.clientTransactionDate = clientTransactionDate;
		this.description = description;
		this.responseMessage = responseMessage;
		this.currency = currency;
		this.paymentAmount = paymentAmount;
		this.ecocashReference = ecocashReference;
		this.transactionStartTime = transactionStartTime;
	}
	
	// ------------------------------------------------------------------
	// Convenience
	// ------------------------------------------------------------------
	
	/**
	 * {@code true} if and only if {@link #getTransactionStatus()} equals
	 * {@link #STATUS_COMPLETED}.
	 */
	public boolean isCompleted() {
		return STATUS_COMPLETED.equals(transactionStatus);
	}
	
	// ------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------
	
	/** Source reference echoed from the original payment. */
	public String getSourceReference() { return sourceReference; }
	
	/** Timestamp when the refund transaction ended on the gateway. */
	public String getTransactionEndTime() { return transactionEndTime; }
	
	/** Callback URL configured for this merchant (may be {@code null}). */
	public String getCallbackUrl() { return callbackUrl; }
	
	/** Destination reference code assigned by the gateway. */
	public String getDestinationReferenceCode() { return destinationReferenceCode; }
	
	/** Source mobile number (customer MSISDN). */
	public String getSourceMobileNumber() { return sourceMobileNumber; }
	
	/**
	 * Status of the refund transaction, typically {@code "COMPLETED"}.
	 * Compare against {@link #STATUS_COMPLETED} or use {@link #isCompleted()}.
	 */
	public String getTransactionStatus() { return transactionStatus; }
	
	/** Refund amount as reported by the gateway. */
	public double getAmount() { return amount; }
	
	/** Ecocash reference on the destination (receiving) side. */
	public String getDestinationEcocashReference() { return destinationEcocashReference; }
	
	/** Merchant code as registered with Ecocash. */
	public String getClientMerchantCode() { return clientMerchantCode; }
	
	/** Merchant number as registered with Ecocash. */
	public String getClientMerchantNumber() { return clientMerchantNumber; }
	
	/** Date on which the client transaction was initiated. */
	public String getClientTransactionDate() { return clientTransactionDate; }
	
	/** Human-readable description returned by the gateway. */
	public String getDescription() { return description; }
	
	/** Response message from the gateway (mirrors {@code transactionStatus}). */
	public String getResponseMessage() { return responseMessage; }
	
	/** Currency code for the refund (e.g. {@code "USD"}). */
	public String getCurrency() { return currency; }
	
	/** Payment amount field as returned by the gateway. */
	public double getPaymentAmount() { return paymentAmount; }
	
	/** Ecocash-internal reference for this refund transaction. */
	public String getEcocashReference() { return ecocashReference; }
	
	/** Timestamp when the refund transaction started on the gateway. */
	public String getTransactionStartTime() { return transactionStartTime; }
	
	@Override
	public String toString() {
		return "RefundResponse{"
				       + "transactionStatus='" + transactionStatus + '\''
				       + ", ecocashReference='" + ecocashReference + '\''
				       + ", amount=" + amount
				       + ", currency='" + currency + '\''
				       + '}';
	}
}