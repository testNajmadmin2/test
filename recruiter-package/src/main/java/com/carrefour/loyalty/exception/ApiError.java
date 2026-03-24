package com.carrefour.loyalty.exception;

import java.time.OffsetDateTime;

public record ApiError(OffsetDateTime timestamp, int status, String message) {
}
