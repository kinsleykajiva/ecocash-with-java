/**
 * Ecocash Open API Java SDK.
 *
 * <h2>Overview</h2>
 * <p>This package provides a type-safe, fluent Java client for the
 * <a href="https://developers.ecocash.co.zw">Ecocash Open API</a>.
 * It supports payment initiation, transaction status polling, and refunds
 * against both sandbox and live environments.</p>
 *
 * <h2>Core classes</h2>
 * <ul>
 *   <li>{@link ecocash.api.EcocashClient} – main entry point; build with
 *       {@link ecocash.api.EcocashClient#builder()}.</li>
 *   <li>{@link ecocash.api.InitPaymentResponse} – receipt from
 *       {@link ecocash.api.EcocashClient#initPayment initPayment}.</li>
 *   <li>{@link ecocash.api.LookupTransactionResponse} – transaction status
 *       from lookup or polling.</li>
 *   <li>{@link ecocash.api.RefundDetails} – input for a refund; build with
 *       {@link ecocash.api.RefundDetails#builder()}.</li>
 *   <li>{@link ecocash.api.RefundResponse} – result of a refund request.</li>
 *   <li>{@link ecocash.api.PollStrategy} – delay strategy for polling.</li>
 *   <li>{@link ecocash.api.PollOptions} – polling configuration; build with
 *       {@link ecocash.api.PollOptions#builder()}.</li>
 *   <li>{@link ecocash.api.EcocashException} – base SDK exception; sub-types
 *       carry HTTP status codes and last-seen responses.</li>
 * </ul>
 *
 * <h2>Minimal example</h2>
 * <pre>{@code
 * try (EcocashClient client = EcocashClient.builder()
 *         .apiKey("your-key")
 *         .merchantCode("your-merchant")
 *         .build()) {
 *
 *     InitPaymentResponse payment =
 *             client.initPayment("26377000000", 20.05, "bread");
 *
 *     LookupTransactionResponse result = client.pollTransaction(payment);
 *
 *     if (result.isPaymentSuccess()) {
 *         System.out.println("Paid: " + result.getEcocashReference());
 *     }
 * }
 * }</pre>
 *
 * <h2>Exception hierarchy</h2>
 * <pre>
 * EcocashException
 *  ├─ EcocashApiException   (non-2xx HTTP response from the gateway)
 *  └─ PollTimeoutException  (polling exhausted all attempts)
 * </pre>
 *
 * <h2>Thread safety</h2>
 * <p>{@link ecocash.api.EcocashClient} is thread-safe after construction and
 * may be shared across threads. All response objects are immutable.</p>
 *
 * <h2>Required dependency</h2>
 * <pre>
 * Maven:  &lt;dependency&gt;
 *           &lt;groupId&gt;org.json&lt;/groupId&gt;
 *           &lt;artifactId&gt;json&lt;/artifactId&gt;
 *           &lt;version&gt;20240303&lt;/version&gt;
 *         &lt;/dependency&gt;
 *
 * Gradle: implementation 'org.json:json:20240303'
 * </pre>
 */
package ecocash.api;