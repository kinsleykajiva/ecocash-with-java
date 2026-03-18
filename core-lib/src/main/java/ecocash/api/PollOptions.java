package ecocash.api;

/**
 * Configuration for the polling behaviour of
 * {@link EcocashClient#pollTransaction(InitPaymentResponse, PollStrategy, PollOptions)}.
 *
 * <p>Build instances with the fluent {@link Builder}:</p>
 *
 * <pre>{@code
 * // Backoff: start at 2 s, double each attempt, up to 8 tries
 * PollOptions options = PollOptions.builder()
 *         .sleepMs(2_000)
 *         .multiplier(2)
 *         .maxAttempts(8)
 *         .build();
 *
 * LookupTransactionResponse result =
 *         client.pollTransaction(payment, PollStrategy.BACKOFF, options);
 * }</pre>
 *
 * <p>When no options are provided the defaults are used:</p>
 * <ul>
 *   <li>maxAttempts = 10</li>
 *   <li>sleepMs = 1 000 ms</li>
 *   <li>multiplier = 2 (only relevant for {@link PollStrategy#BACKOFF})</li>
 * </ul>
 */
public final class PollOptions {
	
	/** Default maximum number of poll attempts. */
	public static final int DEFAULT_MAX_ATTEMPTS = 10;
	
	/** Default sleep interval between polls (milliseconds). */
	public static final long DEFAULT_SLEEP_MS = 1_000L;
	
	/** Default back-off multiplier. */
	public static final int DEFAULT_MULTIPLIER = 2;
	
	// Immutable once built
	private final int maxAttempts;
	private final long sleepMs;
	private final int multiplier;
	
	private PollOptions(Builder b) {
		if (b.maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts must be > 0");
		if (b.sleepMs < 0)      throw new IllegalArgumentException("sleepMs must be >= 0");
		if (b.multiplier <= 0)  throw new IllegalArgumentException("multiplier must be > 0");
		this.maxAttempts = b.maxAttempts;
		this.sleepMs     = b.sleepMs;
		this.multiplier  = b.multiplier;
	}
	
	// ------------------------------------------------------------------
	// Factory
	// ------------------------------------------------------------------
	
	/** Returns a {@link PollOptions} instance populated with the default values. */
	public static PollOptions defaults() {
		return builder().build();
	}
	
	/** Returns a new {@link Builder}. */
	public static Builder builder() {
		return new Builder();
	}
	
	// ------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------
	
	/**
	 * Maximum number of polling attempts before a
	 * {@link EcocashException.PollTimeoutException} is thrown.
	 */
	public int getMaxAttempts() { return maxAttempts; }
	
	/**
	 * Initial sleep duration between polling attempts (milliseconds).
	 * For {@link PollStrategy#BACKOFF} this value is multiplied by
	 * {@link #getMultiplier()} after each attempt.
	 */
	public long getSleepMs() { return sleepMs; }
	
	/**
	 * Back-off multiplier applied to {@code sleepMs} after each attempt.
	 * Only used when the strategy is {@link PollStrategy#BACKOFF}.
	 */
	public int getMultiplier() { return multiplier; }
	
	@Override
	public String toString() {
		return "PollOptions{"
				       + "maxAttempts=" + maxAttempts
				       + ", sleepMs=" + sleepMs
				       + ", multiplier=" + multiplier
				       + '}';
	}
	
	// ------------------------------------------------------------------
	// Builder
	// ------------------------------------------------------------------
	
	/** Fluent builder for {@link PollOptions}. */
	public static final class Builder {
		
		private int  maxAttempts = DEFAULT_MAX_ATTEMPTS;
		private long sleepMs     = DEFAULT_SLEEP_MS;
		private int  multiplier  = DEFAULT_MULTIPLIER;
		
		private Builder() {}
		
		/**
		 * Sets the maximum number of polling attempts.
		 *
		 * @param maxAttempts positive integer
		 * @return this builder
		 */
		public Builder maxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
			return this;
		}
		
		/**
		 * Sets the initial sleep duration between polls (milliseconds).
		 *
		 * @param sleepMs non-negative value in ms
		 * @return this builder
		 */
		public Builder sleepMs(long sleepMs) {
			this.sleepMs = sleepMs;
			return this;
		}
		
		/**
		 * Sets the back-off multiplier (only relevant for {@link PollStrategy#BACKOFF}).
		 *
		 * @param multiplier positive integer
		 * @return this builder
		 */
		public Builder multiplier(int multiplier) {
			this.multiplier = multiplier;
			return this;
		}
		
		/**
		 * Builds an immutable {@link PollOptions}.
		 *
		 * @return configured {@link PollOptions}
		 * @throws IllegalArgumentException if any value violates its constraint
		 */
		public PollOptions build() {
			return new PollOptions(this);
		}
	}
}