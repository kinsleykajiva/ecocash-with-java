# Ecocash Java SDK

A type-safe, fluent Java client for the [Ecocash Open API](https://developers.ecocash.co.zw).
Supports payment initiation, transaction status polling, and refunds against both sandbox and live environments.

> Requires **Java 11+** and `org.json:json:20240303`.

---

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Building a Client](#building-a-client)
- [Initiating a Payment](#initiating-a-payment)
- [Polling for Transaction Status](#polling-for-transaction-status)
  - [Default (Interval)](#default-interval)
  - [Backoff Strategy](#backoff-strategy)
  - [Simple Strategy](#simple-strategy)
- [Single Lookup (No Polling)](#single-lookup-no-polling)
- [Refunding a Payment](#refunding-a-payment)
- [Error Handling](#error-handling)
- [Going Live](#going-live)
- [Class Reference](#class-reference)
- [Package Structure](#package-structure)

---

## Installation

Add the dependency to your build file.

**Maven**
```xml
<dependency>
    <groupId>io.github.kinsleykajiva</groupId>
    <artifactId>ecocash</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**
```groovy
implementation 'io.github.kinsleykajiva:ecocash:0.1.0'
```

### Building from Source

The project uses a Maven multi-module structure. You can build and install the SDK to your local repository:

```bash
mvn clean install
```

---

## Quick Start

```java
try (EcocashClient client = EcocashClient.builder()
        .apiKey("your-api-key")
        .merchantCode("your-merchant-code")
        .build()) {

    // 1. Initiate a payment — sends a prompt to the customer's handset
    InitPaymentResponse payment =
            client.initPayment("26377854266", 20.05, "bread");

    // 2. Poll until SUCCESS or timeout
    LookupTransactionResponse result = client.pollTransaction(payment);

    if (result.isPaymentSuccess()) {
        System.out.println("Confirmed: " + result.getEcocashReference());
    }
}
```

---

## Building a Client

Use the fluent builder. Only `apiKey` and `merchantCode` are required.

```java
EcocashClient client = EcocashClient.builder()
        .apiKey("your-api-key")
        .merchantCode("your-merchant-code")
        .build();
```

`EcocashClient` implements `AutoCloseable`, so the recommended pattern is try-with-resources:

```java
try (EcocashClient client = EcocashClient.builder()
        .apiKey("your-api-key")
        .merchantCode("your-merchant-code")
        .build()) {

    // use client
}
```

### Builder Options

| Method | Default | Description |
|---|---|---|
| `apiKey(String)` | — | **Required.** Your Ecocash API key. |
| `merchantCode(String)` | — | **Required.** Your registered merchant code. |
| `liveMode()` | sandbox | Switches to the live environment. |
| `connectTimeoutSeconds(int)` | 30 | TCP connect timeout. |
| `requestTimeoutSeconds(int)` | 60 | Per-request read timeout. |

---

## Initiating a Payment

```java
InitPaymentResponse payment = client.initPayment(
        "26377854266",  // customer MSISDN
        20.05,          // amount (USD)
        "bread"         // reason shown on customer's handset
);

System.out.println(payment.getSourceReference()); // UUID — use this to poll
System.out.println(payment.getStatus());          // initial gateway status
```

`initPayment` validates inputs immediately — before any network call:

```java
client.initPayment("",     10.00, "reason");  // throws IllegalArgumentException — blank phone
client.initPayment("2637", -1.00, "reason");  // throws IllegalArgumentException — negative amount
client.initPayment("2637", 10.00, "");        // throws IllegalArgumentException — blank reason
```

---

## Polling for Transaction Status

Pass the `InitPaymentResponse` directly from `initPayment` into any of the `pollTransaction` overloads. Polling stops as soon as the status is `SUCCESS`, or throws `PollTimeoutException` when all attempts are exhausted.

### Default (Interval)

The simplest overload. Uses a fixed 1-second interval, up to 10 attempts.

```java
try {
    LookupTransactionResponse result = client.pollTransaction(payment);

    if (result.isPaymentSuccess()) {
        System.out.println("Paid via: " + result.getEcocashReference());
    }
} catch (PollTimeoutException e) {
    System.err.println("Customer did not respond in time.");
    System.err.println("Last status: " + e.getLastResponse().getStatus());
}
```

### Backoff Strategy

Progressively increases the delay after each attempt — ideal for slow approvals and production environments where you want to reduce gateway load.

```java
PollOptions options = PollOptions.builder()
        .sleepMs(2_000)   // start at 2 s
        .multiplier(2)    // double each attempt: 2 s → 4 s → 8 s → 16 s …
        .maxAttempts(5)
        .build();

LookupTransactionResponse result =
        client.pollTransaction(payment, PollStrategy.BACKOFF, options);
```

### Simple Strategy

Polls in a tight loop with no delay between attempts. Only use this in sandbox or test environments.

```java
PollOptions options = PollOptions.builder()
        .maxAttempts(5)
        .build();

LookupTransactionResponse result =
        client.pollTransaction(payment, PollStrategy.SIMPLE, options);
```

### Poll Options Reference

| Method | Default | Description |
|---|---|---|
| `maxAttempts(int)` | 10 | Maximum number of polls before timeout. |
| `sleepMs(long)` | 1000 | Initial sleep between polls (ms). |
| `multiplier(int)` | 2 | Back-off multiplier. Only used with `BACKOFF`. |

---

## Single Lookup (No Polling)

Use `lookupTransaction` when you manage the retry loop yourself — for example inside a scheduled job or webhook-driven flow.

```java
LookupTransactionResponse tx =
        client.lookupTransaction(
                "325a802f-943e-47c2-addf-010285f09cea",  // sourceReference
                "26377854266"                             // customer phone
        );

// Use the STATUS_* constants to avoid hardcoded strings
switch (tx.getStatus()) {
    case LookupTransactionResponse.STATUS_SUCCESS:
        handleSuccess(tx);
        break;
    case LookupTransactionResponse.STATUS_PENDING_VALIDATION:
        scheduleRetry(tx.getReference());
        break;
    default:
        log.warn("Unexpected status: {}", tx.getStatus());
}
```

---

## Refunding a Payment

Build the refund parameters with `RefundDetails.builder()`, then call `refundPayment`.

```java
RefundDetails details = RefundDetails.builder()
        .reference("MP250908.1537.A22242")   // original Ecocash transaction reference
        .phone("263778548266")               // customer MSISDN
        .amount(20.05)
        .clientName("Acme Grocery Store")
        .reason("Defective product — complaint #1234")
        // .currency("USD")                  // optional, defaults to USD
        .build();

RefundResponse refund = client.refundPayment(details);

if (refund.isCompleted()) {
    System.out.println("Refund processed: " + refund.getEcocashReference());
} else {
    System.out.println("Status: " + refund.getTransactionStatus());
}
```

All fields except `currency` are required. `build()` throws `NullPointerException` or `IllegalArgumentException` if any constraint is violated before a network call is made.

---

## Error Handling

All SDK methods declare `throws EcocashException`. Catch the most specific type first.

```java
try {
    InitPaymentResponse payment = client.initPayment(phone, amount, reason);
    LookupTransactionResponse result = client.pollTransaction(payment);

} catch (PollTimeoutException e) {
    // Polling ran out of attempts — inspect the last known state
    LookupTransactionResponse last = e.getLastResponse();
    System.err.println("Timed out. Last status: " + last.getStatus());
    System.err.println("Reference: " + last.getReference());

} catch (EcocashApiException e) {
    // The Ecocash gateway returned a non-2xx HTTP response
    System.err.println("HTTP " + e.getHttpStatus() + ": " + e.getResponseBody());
    // 401 → invalid API key
    // 429 → rate limited
    // 5xx → gateway error

} catch (EcocashException e) {
    // Network timeout, parse failure, interrupted thread, etc.
    System.err.println("SDK error: " + e.getMessage());
}
```

### Exception Hierarchy

```
EcocashException
 ├── EcocashApiException     — non-2xx HTTP response; carries httpStatus + responseBody
 └── PollTimeoutException    — polling exhausted all attempts; carries lastResponse
```

`IllegalArgumentException` is thrown synchronously for invalid inputs (blank phone, negative amount, etc.) and does **not** extend `EcocashException` — no network call is ever made when this fires.

---

## Going Live

Call `.liveMode()` on the builder. Everything else stays the same.

```java
EcocashClient client = EcocashClient.builder()
        .apiKey("your-live-api-key")
        .merchantCode("your-live-merchant-code")
        .liveMode()
        .build();
```

To check at runtime:

```java
client.isLiveMode(); // true
client.getMode();    // "live"
```

---

## Class Reference

### `EcocashClient`
The main entry point. Thread-safe after construction; share a single instance across threads.

| Method | Returns | Description |
|---|---|---|
| `builder()` | `Builder` | Creates a new client builder. |
| `initPayment(phone, amount, reason)` | `InitPaymentResponse` | Sends a C2B payment prompt. |
| `lookupTransaction(reference, phone)` | `LookupTransactionResponse` | Single status check, no retry. |
| `pollTransaction(initResponse)` | `LookupTransactionResponse` | Poll with default INTERVAL strategy. |
| `pollTransaction(initResponse, strategy, options)` | `LookupTransactionResponse` | Poll with custom strategy and options. |
| `refundPayment(details)` | `RefundResponse` | Requests a refund. |
| `isLiveMode()` | `boolean` | Returns `true` when in live mode. |
| `getMode()` | `String` | Returns `"sandbox"` or `"live"`. |
| `close()` | `void` | Releases HTTP client resources. |

---

### `InitPaymentResponse`
Immutable receipt returned by `initPayment`. Pass it to `pollTransaction` or `lookupTransaction`.

| Method | Description |
|---|---|
| `getPhone()` | Customer MSISDN. |
| `getAmount()` | Payment amount. |
| `getCurrency()` | Currency code, typically `"USD"`. |
| `getReason()` | Payment description. |
| `getSourceReference()` | UUID reference for subsequent lookups. |
| `getStatus()` | Initial gateway status (may be `null`). |
| `getEcocashReference()` | Ecocash-internal reference (may be `null` initially). |

---

### `LookupTransactionResponse`
Immutable transaction status returned by lookup and polling calls.

| Method | Description |
|---|---|
| `isPaymentSuccess()` | `true` when status is `SUCCESS`. |
| `getStatus()` | Raw status string from the gateway. |
| `getReference()` | Source reference UUID. |
| `getEcocashReference()` | Ecocash-internal reference, e.g. `"MP250908.1537.A22242"`. |
| `getCustomerMsisdn()` | Customer MSISDN as reported by the gateway. |
| `getAmount()` | `TransactionAmount` (amount + currency), or `null`. |
| `getTransactionDateTime()` | Datetime string, e.g. `"2025-09-08 15:37:16"`. |

**Known status constants:**

```java
LookupTransactionResponse.STATUS_SUCCESS            // "SUCCESS"
LookupTransactionResponse.STATUS_PENDING_VALIDATION // "PENDING_VALIDATION"
```

---

### `RefundDetails`
Immutable input object for `refundPayment`. Always build via `RefundDetails.builder()`.

| Builder Method | Required | Description |
|---|---|---|
| `reference(String)` | Yes | Original Ecocash transaction reference. |
| `phone(String)` | Yes | Customer MSISDN. |
| `amount(double)` | Yes | Amount to refund (must be > 0). |
| `clientName(String)` | Yes | Merchant client name. |
| `reason(String)` | Yes | Human-readable reason for the refund. |
| `currency(String)` | No | Defaults to `"USD"`. |

---

### `RefundResponse`
Immutable result of a refund request.

| Method | Description |
|---|---|
| `isCompleted()` | `true` when `transactionStatus` equals `"COMPLETED"`. |
| `getTransactionStatus()` | Raw status string, typically `"COMPLETED"`. |
| `getEcocashReference()` | Ecocash-internal reference for this refund. |
| `getAmount()` | Refunded amount. |
| `getCurrency()` | Currency code. |
| `getSourceReference()` | Source reference echoed from the original payment. |
| `getResponseMessage()` | Human-readable message from the gateway. |

---

### `PollStrategy`

| Value | Behaviour |
|---|---|
| `INTERVAL` | Fixed delay of `sleepMs` between every attempt. **Default.** |
| `BACKOFF` | Delay starts at `sleepMs` and is multiplied by `multiplier` after each attempt. |
| `SIMPLE` | No delay between attempts. Use only in sandbox/tests. |

---

## Package Structure

```
core-lib/
└── src/main/java/ecocash/api/
    ├── EcocashClient.java              ← main entry point
    ├── EcocashException.java           ← exception hierarchy
    ├── InitPaymentResponse.java        ← payment receipt
    ├── LookupTransactionResponse.java  ← transaction status
    ├── RefundDetails.java              ← refund input (builder)
    ├── RefundResponse.java             ← refund result
    ├── PollStrategy.java               ← polling strategy enum
    ├── PollOptions.java                ← polling configuration (builder)
    └── package-info.java               ← package-level Javadoc

demo/
└── src/main/java/io/github/kinsleykajiva/demo/
    └── DemoApp.java                        ← runnable usage examples
```

---

## License

See [LICENSE](LICENSE) for details.