package com.paul.sprintsync.features.race_session

import com.paul.sprintsync.protocol.NativeProtocol
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SessionClockSyncBinaryRequest(
    val clientSendElapsedNanos: Long,
)

data class SessionClockSyncBinaryResponse(
    val clientSendElapsedNanos: Long,
    val hostReceiveElapsedNanos: Long,
    val hostSendElapsedNanos: Long,
)

object SessionClockSyncBinaryCodec {
    const val VERSION: Byte = 1
    const val TYPE_REQUEST: Byte = 1
    const val TYPE_RESPONSE: Byte = 2

    const val REQUEST_SIZE_BYTES: Int = 10
    const val RESPONSE_SIZE_BYTES: Int = 26

    fun encodeRequest(request: SessionClockSyncBinaryRequest): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            val buffer = ByteBuffer.allocate(REQUEST_SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)
            buffer.put(VERSION)
            buffer.put(TYPE_REQUEST)
            buffer.putLong(request.clientSendElapsedNanos)
            return buffer.array()
        }
        return NativeProtocol.encodeClockSyncRequest(request.clientSendElapsedNanos)
    }

    fun encodeResponse(response: SessionClockSyncBinaryResponse): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            val buffer = ByteBuffer.allocate(RESPONSE_SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)
            buffer.put(VERSION)
            buffer.put(TYPE_RESPONSE)
            buffer.putLong(response.clientSendElapsedNanos)
            buffer.putLong(response.hostReceiveElapsedNanos)
            buffer.putLong(response.hostSendElapsedNanos)
            return buffer.array()
        }
        return NativeProtocol.encodeClockSyncResponse(
            clientSend = response.clientSendElapsedNanos,
            hostReceive = response.hostReceiveElapsedNanos,
            hostSend = response.hostSendElapsedNanos,
        )
    }

    fun decodeRequest(payload: ByteArray): SessionClockSyncBinaryRequest? {
        if (!NativeProtocol.isNativeAvailable()) {
            if (payload.size != REQUEST_SIZE_BYTES) {
                return null
            }
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
            val version = buffer.get()
            val type = buffer.get()
            if (version != VERSION || type != TYPE_REQUEST) {
                return null
            }
            return SessionClockSyncBinaryRequest(clientSendElapsedNanos = buffer.long)
        }

        val clientSendElapsedNanos = NativeProtocol.decodeClockSyncRequest(payload)
        if (clientSendElapsedNanos < 0L) {
            return null
        }
        return SessionClockSyncBinaryRequest(clientSendElapsedNanos = clientSendElapsedNanos)
    }

    fun decodeResponse(payload: ByteArray): SessionClockSyncBinaryResponse? {
        if (!NativeProtocol.isNativeAvailable()) {
            if (payload.size != RESPONSE_SIZE_BYTES) {
                return null
            }
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
            val version = buffer.get()
            val type = buffer.get()
            if (version != VERSION || type != TYPE_RESPONSE) {
                return null
            }
            return SessionClockSyncBinaryResponse(
                clientSendElapsedNanos = buffer.long,
                hostReceiveElapsedNanos = buffer.long,
                hostSendElapsedNanos = buffer.long,
            )
        }

        val decoded = NativeProtocol.decodeClockSyncResponse(payload) ?: return null
        if (decoded.size != 3) {
            return null
        }
        return SessionClockSyncBinaryResponse(
            clientSendElapsedNanos = decoded[0],
            hostReceiveElapsedNanos = decoded[1],
            hostSendElapsedNanos = decoded[2],
        )
    }
}
