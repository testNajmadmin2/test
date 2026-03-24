# Detailed use cases and endpoint behavior

## Purpose of this document

This document explains, in detail, how each use case of the loyalty application works.
The goal is to remove ambiguity from the API behavior and from the endpoint verification results.

A very important clarification:

- the application manipulates **loyalty points**,
- purchase requests are sent in **euro cents**,
- API balances are returned in **points**, not in euros and not in cents.

So when the API returns `120`, it means **120 loyalty points**.
It does **not** mean `120 cents`.

---

## Core business rules used everywhere

The following rules drive every use case:

1. **1 point is earned for each 100 cents spent**.
   - `100 cents = 1 point`
   - `12_500 cents = 125 points`
2. Each earn action creates a new **point bucket**.
3. A point bucket is valid for **365 days**.
4. Spending uses **FIFO**: the oldest earned bucket is consumed first.
5. Expired points cannot be spent.
6. The `voucher` endpoint always consumes a fixed **100 points**.
7. The `donate` endpoint consumes the number of points sent in the request.
8. The `balance` response always exposes:
   - the customer id,
   - the currently available points,
   - the remaining buckets.
9. The `expiring-soon` endpoint does not send a real notification.
   It returns the buckets that should be considered "to notify soon".
10. In this kata, "expiring soon" means **within the next 30 days**.

These rules are implemented in the service and domain layers. See `LoyaltyService`, `CustomerLoyaltyAccount`, and `PointBucket`. 

---

## Data vocabulary

### Purchase amount
A purchase amount is sent in **cents**.

Example:
- `12500` means `125.00 EUR`

### Available points
`availablePoints` is the number of usable loyalty points remaining for the customer.

Example:
- `125` means the customer can still spend **125 loyalty points**

### Bucket
A bucket represents a batch of points earned at the same time.

Example bucket:

```json
{
  "id": "...",
  "remainingPoints": 125,
  "earnedDate": "2026-03-23",
  "expirationDate": "2027-03-23"
}
```

This means:
- 125 points are still available in this bucket,
- they were earned on March 23, 2026,
- they expire on March 23, 2027.

---

## Use case 1 - Read balance for an unknown customer

### Endpoint
`GET /customers/{customerId}/points/balance`

### Example
`GET /customers/cust-e2e/points/balance`

### Observed result
HTTP `404`

```json
{
  "timestamp": "...",
  "status": 404,
  "message": "Customer 'cust-e2e' was not found"
}
```

### Why this happens
At the beginning, the customer does not exist in the in-memory repository.
The application creates an account automatically only when the customer earns points for the first time.
So reading the balance before any earn operation returns a business `404`.

### Meaning
This does **not** mean the endpoint is broken.
It means the endpoint is working correctly and enforcing the rule:
**no account exists yet for this customer**.

---

## Use case 2 - Earn points

### Endpoint
`POST /customers/{customerId}/points/earn`

### Example request

```json
{
  "purchaseAmountInCents": 12500
}
```

### How it is calculated
The application uses this formula:

```text
points = purchaseAmountInCents / 100
```

So:

```text
12500 / 100 = 125
```

### Meaning
A purchase of **12,500 cents**, which is **125.00 EUR**, creates **125 loyalty points**.

### Observed result
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "availablePoints": 125,
  "buckets": [
    {
      "id": "...",
      "remainingPoints": 125,
      "earnedDate": "2026-03-23",
      "expirationDate": "2027-03-23"
    }
  ]
}
```

### What this response means exactly
- `availablePoints: 125` means **the customer now owns 125 usable loyalty points**,
- `remainingPoints: 125` means the created bucket still contains all 125 points,
- `earnedDate` is the date of the earn action,
- `expirationDate` is 365 days later.

### Important clarification
When we say:

> earning 12500 cents creates 125 points

we mean:
- input unit = **cents**,
- output unit = **loyalty points**.

---

## Use case 3 - Read balance after earning

### Endpoint
`GET /customers/{customerId}/points/balance`

### Observed result after the previous earn
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "availablePoints": 125,
  "buckets": [
    {
      "remainingPoints": 125,
      "earnedDate": "2026-03-23",
      "expirationDate": "2027-03-23"
    }
  ]
}
```

### Why the balance is 125
Nothing has been consumed yet.
Nothing has expired yet.
So the customer still has all **125 points**.

### Meaning of "balance direct returns 125"
This means:
- the customer balance endpoint returns **125 loyalty points available**.
- It does **not** mean `125 cents` or `125 EUR`.

---

## Use case 4 - Spend points as payment

### Endpoint
`POST /customers/{customerId}/points/spend`

### Example request

```json
{
  "points": 5,
  "spendType": "PAYMENT"
}
```

### What happens
The customer already has `125` points.
The request asks to spend `5` points as a payment usage.

The service computes:

```text
actualPoints = 5
```

Then the domain consumes the oldest available bucket first.
Here there is only one bucket, so it simply removes 5 points from that bucket.

### Calculation

```text
125 - 5 = 120
```

### Observed result
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "availablePoints": 120,
  "buckets": [
    {
      "remainingPoints": 120,
      "earnedDate": "2026-03-23",
      "expirationDate": "2027-03-23"
    }
  ]
}
```

### What "spending 5 payment points returns 120" means
It means:
- 5 loyalty points were consumed,
- the customer now has **120 loyalty points left**.

So `120` is a number of **points**, not cents.

---

## Use case 5 - Convert points into a voucher

### Endpoint
`POST /customers/{customerId}/points/voucher`

### Important rule
The voucher endpoint does **not** accept a number from the caller.
It always consumes a fixed amount:

```text
100 points
```

### Situation before the call
After the payment spend, the customer has `120` points left.

### Calculation
Voucher cost = `100 points`

```text
120 - 100 = 20
```

### Observed result
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "availablePoints": 20,
  "buckets": [
    {
      "remainingPoints": 20,
      "earnedDate": "2026-03-23",
      "expirationDate": "2027-03-23"
    }
  ]
}
```

### What "voucher conversion returns 20" means
It means:
- the voucher consumed **100 loyalty points**,
- the customer now has **20 loyalty points remaining**.

So `20` means **20 points left**.
It does **not** mean `20 cents` and it does **not** mean voucher value in money.

---

## Use case 6 - Donate points

### Endpoint
`POST /customers/{customerId}/points/donate`

### Example request

```json
{
  "points": 10
}
```

### Situation before the call
After voucher conversion, the customer has `20` points left.

### Calculation
The donation endpoint consumes exactly the number of points sent in the request.

```text
20 - 10 = 10
```

### Observed result
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "availablePoints": 10,
  "buckets": [
    {
      "remainingPoints": 10,
      "earnedDate": "2026-03-23",
      "expirationDate": "2027-03-23"
    }
  ]
}
```

### What "donating 10 returns 10" means
It means:
- 10 loyalty points were donated,
- 10 loyalty points remain available after the donation.

So the returned `10` is again **a number of points**.
It is **not** 10 cents.

---

## Use case 7 - List buckets expiring soon

### Endpoint
`GET /customers/{customerId}/points/expiring-soon`

### Important rule
A bucket is considered expiring soon if its expiration date is within the next **30 days**.

### Situation in our verification scenario
The bucket was just created on `2026-03-23`.
It expires on `2027-03-23`, which is about **365 days later**.
So it is **not** expiring within 30 days.

### Observed result
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "availablePoints": 10,
  "buckets": []
}
```

### What this means
- `availablePoints: 10` means the customer still has 10 usable points,
- `buckets: []` means **there is no bucket to notify yet**, because the current bucket is too far from expiration.

### What "expiring-soon returns an empty bucket list for a fresh bucket" means
It means that a newly created bucket is not near expiration, so the notification list is empty.

This endpoint is a **notification preview** endpoint, not a real email sender.

---

## Use case 8 - Explicit expiration

### Endpoint
`POST /customers/{customerId}/points/expire`

### Purpose
This endpoint forces the application to apply expiration logic "now".
It is useful for the kata because there is no scheduler or background batch.

### Situation in our verification scenario
The bucket is fresh and expires in 365 days.
So when the endpoint checks expiration now, nothing should expire.

### Observed result
HTTP `200`

```json
{
  "customerId": "cust-e2e",
  "expiredPoints": 0,
  "availablePoints": 10
}
```

### What this means
- `expiredPoints: 0` means **zero points became expired during this call**,
- `availablePoints: 10` means the customer still has 10 usable points.

### What "explicit expiration returns 0 expired points" means
It means the expiration process ran successfully, but no bucket had reached its expiration date yet.

---

## Use case 9 - Overspend / insufficient points

### Endpoint
`POST /customers/{customerId}/points/spend`

### Example request

```json
{
  "points": 999,
  "spendType": "PAYMENT"
}
```

### Situation before the call
The customer has only `10` points left.

### What happens
The domain checks whether the available balance is enough.
It is not.
So the application throws an insufficient-points business exception.

### Observed result
HTTP `400`

```json
{
  "timestamp": "...",
  "status": 400,
  "message": "Customer 'cust-e2e' does not have enough points to spend 999 points"
}
```

### Important clarification
Earlier, the summary said "expected 404 insufficient point error".
That was not correct.
The actual behavior is:

- **404** when the customer does not exist,
- **400** when the customer exists but does not have enough points.

### Why 400 is correct here
The request format is valid.
The customer exists.
But the requested business action is invalid because the balance is insufficient.
So the application returns a business validation error.

---

## Full scenario recap with units

Here is the same scenario with explicit units at every step:

1. Unknown customer balance
   - result: `404 customer not found`

2. Earn `12500 cents`
   - conversion: `12500 cents = 125 points`
   - new balance: `125 points`

3. Read balance
   - result: `125 points`

4. Spend `5 points` as payment
   - previous balance: `125 points`
   - new balance: `120 points`

5. Create voucher
   - voucher cost: `100 points`
   - previous balance: `120 points`
   - new balance: `20 points`

6. Donate `10 points`
   - previous balance: `20 points`
   - new balance: `10 points`

7. Expiring soon
   - current bucket expires in about `365 days`
   - notification window = `30 days`
   - result: empty list

8. Explicit expiration now
   - no bucket has reached its expiration date
   - expired points: `0`
   - balance remains `10 points`

9. Overspend `999 points`
   - available balance = `10 points`
   - result: `400 insufficient points`

---

## Which class does what

### Controller layer
`PointsController`
- exposes HTTP endpoints,
- receives JSON requests,
- returns JSON responses.

### Service layer
`LoyaltyService`
- converts cents to points,
- creates accounts on first earn,
- decides voucher fixed cost,
- triggers expiration before balance/spend operations,
- maps business objects to API DTOs.

### Domain model layer
`CustomerLoyaltyAccount`
- holds the list of buckets,
- computes available points,
- applies FIFO consumption,
- applies expiration,
- finds buckets expiring soon.

`PointBucket`
- stores one batch of points,
- knows how many points remain,
- knows whether it is expired,
- consumes points,
- marks remaining points as expired when necessary.

### Repository layer
`InMemoryLoyaltyAccountRepository`
- stores accounts in memory for the kata,
- keeps the project small and fast to explain.

---

## Final clarification about the numbers in responses

When you read numbers in the API responses:

- `purchaseAmountInCents` = **money input in cents**,
- `availablePoints` = **loyalty points remaining**,
- `remainingPoints` = **points left in one bucket**,
- `expiredPoints` = **points that became unusable because of expiration**.

So:
- `12500` is **cents**,
- `125`, `120`, `20`, `10` are **points**,
- `0` in `expiredPoints` means **no point expired during that expiration call**.
