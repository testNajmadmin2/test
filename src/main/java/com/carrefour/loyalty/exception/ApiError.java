package com.carrefour.loyalty.exception;

import java.time.OffsetDateTime;

/**
 * Small error payload returned by the API.
 */
public record ApiError(OffsetDateTime timestamp, int status, String message) {
}
