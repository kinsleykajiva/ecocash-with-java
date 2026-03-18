package ecocash.api;

import java.util.Objects;

/**
 * Input parameters for a refund request.
 *
 * <p>Construct instances using the fluent {@link Builder}:</p>
 *
 * <pre>{@code
 * RefundDetails details = RefundDetails.builder()
 *         .reference("MP250908.1537.A22242")
 *         .phone("26377000000")
 *         .amount(20.05)
 *         .clientName("Acme Store")
 *         .reason("The bread was rotten")
 *         .build();
 *
 * RefundResponse refund = client.refundPayment(details);
 * }</pre>
 */
public final class RefundDetails {
	
	private final String reference;
	private final String phone;
	private final double amount;
	private final String clientName;
	private final String reason;
	private final String currency;
	
	private RefundDetails(Builder builder) {
		this.reference  = Objects.requireNonNull(builder.reference,  "reference is required");
		this.phone      = Objects.requireNonNull(builder.phone,      "phone is required");
		this.clientName = Objects.requireNonNull(builder.clientName, "clientName is required");
		this.reason     = Objects.requireNonNull(builder.reason,     "reason is required");
		this.currency   = builder.currency != null ? builder.currency : "USD";
		if (builder.amount <= 0) throw new IllegalArgumentException("amount must be > 0");
		this.amount = builder.amount;
	}
	
	// ------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------
	
	/** The original Ecocash transaction reference to be reversed. */
	public String getReference() { return reference; }
	
	/** The customer's MSISDN (source mobile number). */
	public String getPhone() { return phone; }
	
	/** Amount to refund — must match or be less than the original transaction. */
	public double getAmount() { return amount; }
	
	/** Merchant client name. */
	public String getClientName() { return clientName; }
	
	/** Human-readable reason for the refund. */
	public String getReason() { return reason; }
	
	/** Currency code (default: {@code "USD"}). */
	public String getCurrency() { return currency; }
	
	// ------------------------------------------------------------------
	// Builder
	// ------------------------------------------------------------------
	
	/**
	 * Creates a new {@link Builder} for {@link RefundDetails}.
	 *
	 * @return a fresh builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * Fluent builder for {@link RefundDetails}.
	 */
	public static final class Builder {
		
		private String reference;
		private String phone;
		private double amount;
		private String clientName;
		private String reason;
		private String currency;
		
		private Builder() {}
		
		/**
		 * Sets the original Ecocash transaction reference to be refunded.
		 *
		 * @param reference the original transaction reference; must not be {@code null}
		 * @return this builder
		 */
		public Builder reference(String reference) {
			this.reference = reference;
			return this;
		}
		
		/**
		 * Sets the customer's phone / MSISDN.
		 *
		 * @param phone MSISDN e.g. {@code "26377000000"}; must not be {@code null}
		 * @return this builder
		 */
		public Builder phone(String phone) {
			this.phone = phone;
			return this;
		}
		
		/**
		 * Sets the refund amount (must be positive).
		 *
		 * @param amount positive monetary value
		 * @return this builder
		 */
		public Builder amount(double amount) {
			this.amount = amount;
			return this;
		}
		
		/**
		 * Sets the merchant client name sent to the gateway.
		 *
		 * @param clientName non-null name
		 * @return this builder
		 */
		public Builder clientName(String clientName) {
			this.clientName = clientName;
			return this;
		}
		
		/**
		 * Sets a human-readable reason for the refund.
		 *
		 * @param reason non-null reason string
		 * @return this builder
		 */
		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}
		
		/**
		 * Overrides the currency code (defaults to {@code "USD"} when not set).
		 *
		 * @param currency ISO-4217 currency code
		 * @return this builder
		 */
		public Builder currency(String currency) {
			this.currency = currency;
			return this;
		}
		
		/**
		 * Builds the {@link RefundDetails} instance.
		 *
		 * @return immutable {@link RefundDetails}
		 * @throws NullPointerException     if any required field is {@code null}
		 * @throws IllegalArgumentException if {@code amount} is not positive
		 */
		public RefundDetails build() {
			return new RefundDetails(this);
		}
	}
	
	@Override
	public String toString() {
		return "RefundDetails{"
				       + "reference='" + reference + '\''
				       + ", phone='" + phone + '\''
				       + ", amount=" + amount
				       + ", clientName='" + clientName + '\''
				       + ", reason='" + reason + '\''
				       + ", currency='" + currency + '\''
				       + '}';
	}
}