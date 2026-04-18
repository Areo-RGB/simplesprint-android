package com.paul.simplesprint.protocol

object NativeProtocol {
    private val nativeLoaded: Boolean =
        runCatching {
            System.loadLibrary("sprint_sync_protocol_jni")
        }.isSuccess

    fun isNativeAvailable(): Boolean = nativeLoaded

    fun encodeClockSyncRequest(clientSendElapsedNanos: Long): ByteArray {
        return if (nativeLoaded) {
            encodeClockSyncRequestNative(clientSendElapsedNanos)
        } else {
            ByteArray(0)
        }
    }

    fun decodeClockSyncRequest(payload: ByteArray): Long {
        return if (nativeLoaded) {
            decodeClockSyncRequestNative(payload)
        } else {
            -1L
        }
    }

    fun encodeClockSyncResponse(clientSend: Long, hostReceive: Long, hostSend: Long): ByteArray {
        return if (nativeLoaded) {
            encodeClockSyncResponseNative(clientSend, hostReceive, hostSend)
        } else {
            ByteArray(0)
        }
    }

    fun decodeClockSyncResponse(payload: ByteArray): LongArray? {
        return if (nativeLoaded) {
            decodeClockSyncResponseNative(payload)
        } else {
            null
        }
    }

    fun encodeTelemetryEnvelope(typeTag: Int, jsonPayload: String): ByteArray? {
        return if (nativeLoaded) {
            encodeTelemetryEnvelopeNative(typeTag, jsonPayload)
        } else {
            null
        }
    }

    fun decodeTelemetryEnvelope(payload: ByteArray): String? {
        return if (nativeLoaded) {
            decodeTelemetryEnvelopeNative(payload)
        } else {
            null
        }
    }

    private external fun encodeClockSyncRequestNative(clientSendElapsedNanos: Long): ByteArray

    private external fun decodeClockSyncRequestNative(payload: ByteArray): Long

    private external fun encodeClockSyncResponseNative(clientSend: Long, hostReceive: Long, hostSend: Long): ByteArray

    private external fun decodeClockSyncResponseNative(payload: ByteArray): LongArray?

    private external fun encodeTelemetryEnvelopeNative(typeTag: Int, jsonPayload: String): ByteArray?

    private external fun decodeTelemetryEnvelopeNative(payload: ByteArray): String?
}
