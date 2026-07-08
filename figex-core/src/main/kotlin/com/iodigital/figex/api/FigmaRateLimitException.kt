package com.iodigital.figex.api

import kotlin.time.Duration

class FigmaRateLimitException(
    val retryAfter: Duration,
    val planTier: String?,
    val rateLimitType: String?,
) : RuntimeException(
    buildString {
        appendLine("Figma API rate limit reached. Retry after: $retryAfter")
        appendLine()
        appendLine("  Plan tier: ${planTier ?: "unknown"}")
        appendLine("  Seat type: ${rateLimitType ?: "unknown"} (low = View/Collab, high = Dev/Full Design)")
        appendLine()
        if (rateLimitType == "low") {
            appendLine("Your token belongs to a user with a View or Collab seat, which has very")
            appendLine("restrictive rate limits (as low as 6 requests/month for file endpoints).")
            appendLine("Use a token from a user with a Dev or Full Design seat for higher limits.")
        } else {
            appendLine("Consider reducing the number of parallel requests or waiting before retrying.")
        }
    }
)