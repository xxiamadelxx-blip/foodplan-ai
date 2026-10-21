package com.foodplan.ai

/**
 * Bounded, automatic read-only retailer search refresh policy.
 *
 * Navigation back to the Products tab is the retry opportunity. There is no
 * background polling, no retry loop, and no bypass of robots/CAPTCHA/HTTP denial.
 * An incomplete or inaccessible search must not be cached forever. A complete
 * name-only discovery needs less frequent refresh; neither result is price proof.
 */
internal object PublicSearchRefreshPolicy {
    const val FAILURE_RETRY_MS = 5L * 60L * 1000L
    const val COMPLETE_REFRESH_MS = 60L * 60L * 1000L

    fun shouldRescan(
        shoppingKey: String,
        previousKey: String?,
        lastFinishedEpochMs: Long,
        previousComplete: Boolean,
        nowEpochMs: Long
    ): Boolean {
        if (shoppingKey.isBlank()) return false
        if (previousKey != shoppingKey || lastFinishedEpochMs <= 0L ||
            nowEpochMs < lastFinishedEpochMs) return true
        val cooldown = if (previousComplete) COMPLETE_REFRESH_MS else FAILURE_RETRY_MS
        return nowEpochMs - lastFinishedEpochMs >= cooldown
    }
}
