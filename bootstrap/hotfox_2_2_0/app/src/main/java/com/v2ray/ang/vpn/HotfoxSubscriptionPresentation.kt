package com.v2ray.ang.vpn

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class SubscriptionPresentation(
    val titleIsPremium: Boolean,
    val status: Status,
    val expiryLabel: String?,
    val remainingLabel: String?,
    val remainingDays: Int?,
    val timelineFraction: Float?,
) {
    enum class Status { MISSING, UNKNOWN, ACTIVE, EXPIRED }
}

object HotfoxSubscriptionPresentation {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US)

    fun fromExpiryEpochSeconds(
        expireAtEpochSeconds: Long?,
        serverCount: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): SubscriptionPresentation {
        if (expireAtEpochSeconds == null || expireAtEpochSeconds <= 0L) {
            return SubscriptionPresentation(
                titleIsPremium = false,
                status = if (serverCount > 0) SubscriptionPresentation.Status.UNKNOWN else SubscriptionPresentation.Status.MISSING,
                expiryLabel = null,
                remainingLabel = null,
                remainingDays = null,
                timelineFraction = null,
            )
        }
        val expiryInstant = Instant.ofEpochSecond(expireAtEpochSeconds)
        val expiryDate = expiryInstant.atZone(zoneId).toLocalDate()
        val today = now.atZone(zoneId).toLocalDate()
        val calendarRemaining = remainingDays(today, expiryDate)
        val expired = !now.isBefore(expiryInstant)
        return if (expired) {
            SubscriptionPresentation(
                titleIsPremium = false,
                status = SubscriptionPresentation.Status.EXPIRED,
                expiryLabel = dateFormat.format(expiryDate),
                remainingLabel = "0",
                remainingDays = 0,
                timelineFraction = 1f,
            )
        } else {
            SubscriptionPresentation(
                titleIsPremium = true,
                status = SubscriptionPresentation.Status.ACTIVE,
                expiryLabel = dateFormat.format(expiryDate),
                remainingLabel = calendarRemaining.toString(),
                remainingDays = calendarRemaining,
                timelineFraction = null,
            )
        }
    }

    fun remainingDays(today: LocalDate, expiry: LocalDate): Int =
        maxOf(0, ChronoUnit.DAYS.between(today, expiry).toInt())
}
