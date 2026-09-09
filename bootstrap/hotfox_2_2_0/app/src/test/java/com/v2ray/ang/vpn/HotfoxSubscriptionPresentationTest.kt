package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HotfoxSubscriptionPresentationTest {
    private val moscow = ZoneId.of("Europe/Moscow")

    @Test
    fun futureExpiryCountsRemainingDaysInLocalZone() {
        val expiry = LocalDate.of(2026, 10, 5).atStartOfDay(moscow).toInstant()
        val now = LocalDate.of(2026, 9, 7).atStartOfDay(moscow).toInstant()
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = expiry.epochSecond,
            serverCount = 12,
            zoneId = moscow,
            now = now,
        )
        assertEquals(SubscriptionPresentation.Status.ACTIVE, shown.status)
        assertEquals("05.10.2026", shown.expiryLabel)
        assertEquals(28, shown.remainingDays)
    }

    @Test
    fun expiryTodayShowsZeroDays() {
        val today = LocalDate.of(2026, 9, 8)
        assertEquals(0, HotfoxSubscriptionPresentation.remainingDays(today, today))
    }

    @Test
    fun expiredNeverShowsNegativeDays() {
        val expiry = Instant.parse("2026-01-01T00:00:00Z")
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = expiry.epochSecond,
            serverCount = 4,
            zoneId = moscow,
            now = Instant.parse("2026-09-08T00:00:00Z"),
        )
        assertEquals(SubscriptionPresentation.Status.EXPIRED, shown.status)
        assertEquals(0, shown.remainingDays)
    }

    @Test
    fun pastExpiryEarlierTodayIsExpiredNotPremium() {
        val expiry = LocalDate.of(2026, 9, 9).atTime(10, 0).atZone(moscow).toInstant()
        val now = LocalDate.of(2026, 9, 9).atTime(11, 0).atZone(moscow).toInstant()
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = expiry.epochSecond,
            serverCount = 3,
            zoneId = moscow,
            now = now,
        )
        assertEquals(SubscriptionPresentation.Status.EXPIRED, shown.status)
        assertEquals(false, shown.titleIsPremium)
        assertEquals(0, shown.remainingDays)
        assertEquals("09.09.2026", shown.expiryLabel)
    }

    @Test
    fun exactExpiryInstantIsExpired() {
        val expiry = LocalDate.of(2026, 9, 9).atTime(12, 0).atZone(moscow).toInstant()
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = expiry.epochSecond,
            serverCount = 3,
            zoneId = moscow,
            now = expiry,
        )
        assertEquals(SubscriptionPresentation.Status.EXPIRED, shown.status)
        assertEquals(false, shown.titleIsPremium)
        assertEquals(0, shown.remainingDays)
    }

    @Test
    fun futureExpiryLaterTodayStaysActiveWithZeroRemainingDays() {
        val expiry = LocalDate.of(2026, 9, 9).atTime(23, 0).atZone(moscow).toInstant()
        val now = LocalDate.of(2026, 9, 9).atTime(11, 0).atZone(moscow).toInstant()
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = expiry.epochSecond,
            serverCount = 3,
            zoneId = moscow,
            now = now,
        )
        assertEquals(SubscriptionPresentation.Status.ACTIVE, shown.status)
        assertEquals(true, shown.titleIsPremium)
        assertEquals(0, shown.remainingDays)
        assertEquals("09.09.2026", shown.expiryLabel)
    }

    @Test
    fun unknownExpiryDoesNotInventPremium() {
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(0L, serverCount = 8)
        assertEquals(SubscriptionPresentation.Status.UNKNOWN, shown.status)
        assertNull(shown.expiryLabel)
    }
}
