package ecocash.api;

/**
 * Determines the delay behaviour between successive transaction status polls.
 *
 * <p>Pass one of these constants to
 * {@link EcocashClient#pollTransaction(InitPaymentResponse, PollStrategy, PollOptions)}:
 *
 * <pre>{@code
 * // Fixed 3-second gap, up to 12 attempts
 * PollOptions opts = PollOptions.builder().sleepMs(3_000).maxAttempts(12).build();
 * LookupTransactionResponse tx = client.pollTransaction(payment, PollStrategy.INTERVAL, opts);
 * }</pre>
 */
public enum PollStrategy {
	
	/**
	 * Poll at a fixed interval defined by {@link PollOptions#getSleepMs()}.
	 *
	 * <p>This is the <strong>default</strong> strategy when none is specified.</p>
	 */
	INTERVAL,
	
	/**
	 * Poll with exponential back-off.
	 *
	 * <p>The initial sleep is {@link PollOptions#getSleepMs()}; after each
	 * attempt it is multiplied by {@link PollOptions#getMultiplier()}.
	 * Use this to avoid hammering the gateway during slow payment approval.</p>
	 */
	BACKOFF,
	
	/**
	 * Poll in a tight loop with no imposed delay between attempts.
	 *
	 * <p>Only recommended for testing environments or when very low latency
	 * is required and the gateway explicitly supports rapid polling.</p>
	 */
	SIMPLE
}