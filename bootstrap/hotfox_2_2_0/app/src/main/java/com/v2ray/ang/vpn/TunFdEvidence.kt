package com.v2ray.ang.vpn

/**
 * TUN ParcelFileDescriptor lifetime vs HEV handoff.
 * Presence of establish() in source is not 2.9 proof that HEV received a live fd.
 */
object TunFdEvidence {
    @Volatile
    var established: Boolean = false
        private set

    @Volatile
    var hevReceived: Boolean = false
        private set

    @Volatile
    var hevStoppedBeforeClose: Boolean = false
        private set

    @Volatile
    var closed: Boolean = false
        private set

    fun resetForTests() {
        established = false
        hevReceived = false
        hevStoppedBeforeClose = false
        closed = false
    }

    fun recordEstablish() {
        established = true
        hevReceived = false
        hevStoppedBeforeClose = false
        closed = false
    }

    fun recordHevReceived() {
        hevReceived = established && !closed
    }

    fun recordHevStopped() {
        if (established && !closed) {
            hevStoppedBeforeClose = true
        }
    }

    fun recordClosed() {
        closed = true
    }

    fun summary(): String =
        "tunEstablished=$established hevReceivedFd=$hevReceived hevStoppedBeforeClose=$hevStoppedBeforeClose tunClosed=$closed"
}
