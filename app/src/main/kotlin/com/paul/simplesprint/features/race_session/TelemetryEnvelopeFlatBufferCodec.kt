package com.paul.simplesprint.features.race_session

import com.paul.simplesprint.protocol.NativeProtocol
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface DecodedTelemetryEnvelope {
    data class TriggerRequest(val message: SessionTriggerRequestMessage) : DecodedTelemetryEnvelope

    data class Trigger(val message: SessionTriggerMessage) : DecodedTelemetryEnvelope

    data class TimelineSnapshot(val message: SessionTimelineSnapshotMessage) : DecodedTelemetryEnvelope

    data class Snapshot(val message: SessionSnapshotMessage) : DecodedTelemetryEnvelope

    data class TriggerRefinementEnvelope(val message: SessionTriggerRefinementMessage) : DecodedTelemetryEnvelope

    data class ConfigUpdate(val message: SessionDeviceConfigUpdateMessage) : DecodedTelemetryEnvelope

    data class ClockResync(val message: SessionClockResyncRequestMessage) : DecodedTelemetryEnvelope

    data class Identity(val message: SessionDeviceIdentityMessage) : DecodedTelemetryEnvelope

    data class DeviceTelemetryEnvelope(val message: SessionDeviceTelemetryMessage) : DecodedTelemetryEnvelope

    data class LapResultEnvelope(val message: SessionLapResultMessage) : DecodedTelemetryEnvelope
}

object TelemetryEnvelopeFlatBufferCodec {
    private const val TYPE_TAG_TRIGGER_REQUEST = 1
    private const val TYPE_TAG_SESSION_TRIGGER = 2
    private const val TYPE_TAG_TIMELINE_SNAPSHOT = 3
    private const val TYPE_TAG_SESSION_SNAPSHOT = 4
    private const val TYPE_TAG_TRIGGER_REFINEMENT = 5
    private const val TYPE_TAG_DEVICE_CONFIG_UPDATE = 6
    private const val TYPE_TAG_CLOCK_RESYNC_REQUEST = 7
    private const val TYPE_TAG_DEVICE_IDENTITY = 8
    private const val TYPE_TAG_DEVICE_TELEMETRY = 9
    private const val TYPE_TAG_LAP_RESULT = 10
    private const val FALLBACK_PREFIX = "telemetry-fallback:"

    private val fallbackMessagesById = ConcurrentHashMap<String, DecodedTelemetryEnvelope>()

    fun encodeTriggerRequest(message: SessionTriggerRequestMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.TriggerRequest(message))
        }
        return encodeEnvelope(TYPE_TAG_TRIGGER_REQUEST, message.toJsonString())
    }

    fun encodeTrigger(message: SessionTriggerMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.Trigger(message))
        }
        return encodeEnvelope(TYPE_TAG_SESSION_TRIGGER, message.toJsonString())
    }

    fun encodeTimelineSnapshot(message: SessionTimelineSnapshotMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.TimelineSnapshot(message))
        }
        return encodeEnvelope(TYPE_TAG_TIMELINE_SNAPSHOT, message.toJsonString())
    }

    fun encodeSnapshot(message: SessionSnapshotMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.Snapshot(message))
        }
        return encodeEnvelope(TYPE_TAG_SESSION_SNAPSHOT, message.toJsonString())
    }

    fun encodeTriggerRefinement(message: SessionTriggerRefinementMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.TriggerRefinementEnvelope(message))
        }
        return encodeEnvelope(TYPE_TAG_TRIGGER_REFINEMENT, message.toJsonString())
    }

    fun encodeDeviceConfigUpdate(message: SessionDeviceConfigUpdateMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.ConfigUpdate(message))
        }
        return encodeEnvelope(TYPE_TAG_DEVICE_CONFIG_UPDATE, message.toJsonString())
    }

    fun encodeClockResyncRequest(message: SessionClockResyncRequestMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.ClockResync(message))
        }
        return encodeEnvelope(TYPE_TAG_CLOCK_RESYNC_REQUEST, message.toJsonString())
    }

    fun encodeDeviceIdentity(message: SessionDeviceIdentityMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.Identity(message))
        }
        return encodeEnvelope(TYPE_TAG_DEVICE_IDENTITY, message.toJsonString())
    }

    fun encodeDeviceTelemetry(message: SessionDeviceTelemetryMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.DeviceTelemetryEnvelope(message))
        }
        return encodeEnvelope(TYPE_TAG_DEVICE_TELEMETRY, message.toJsonString())
    }

    fun encodeLapResult(message: SessionLapResultMessage): ByteArray {
        if (!NativeProtocol.isNativeAvailable()) {
            return encodeFallbackEnvelope(DecodedTelemetryEnvelope.LapResultEnvelope(message))
        }
        return encodeEnvelope(TYPE_TAG_LAP_RESULT, message.toJsonString())
    }

    fun decode(payloadBytes: ByteArray): DecodedTelemetryEnvelope? {
        if (!NativeProtocol.isNativeAvailable()) {
            val fallbackKey = payloadBytes.toString(Charsets.UTF_8)
            if (!fallbackKey.startsWith(FALLBACK_PREFIX)) {
                return null
            }
            return fallbackMessagesById[fallbackKey.removePrefix(FALLBACK_PREFIX)]
        }

        val jsonPayload = NativeProtocol.decodeTelemetryEnvelope(payloadBytes) ?: return null
        return SessionTriggerRequestMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.TriggerRequest(it) }
            ?: SessionTriggerMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.Trigger(it) }
            ?: SessionTimelineSnapshotMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.TimelineSnapshot(it) }
            ?: SessionSnapshotMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.Snapshot(it) }
            ?: SessionTriggerRefinementMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.TriggerRefinementEnvelope(it) }
            ?: SessionDeviceConfigUpdateMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.ConfigUpdate(it) }
            ?: SessionClockResyncRequestMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.ClockResync(it) }
            ?: SessionDeviceIdentityMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.Identity(it) }
            ?: SessionDeviceTelemetryMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.DeviceTelemetryEnvelope(it) }
            ?: SessionLapResultMessage.tryParse(jsonPayload)?.let { DecodedTelemetryEnvelope.LapResultEnvelope(it) }
    }

    private fun encodeFallbackEnvelope(decoded: DecodedTelemetryEnvelope): ByteArray {
        val id = UUID.randomUUID().toString()
        fallbackMessagesById[id] = decoded
        return (FALLBACK_PREFIX + id).toByteArray(Charsets.UTF_8)
    }

    private fun encodeEnvelope(typeTag: Int, jsonPayload: String): ByteArray {
        return NativeProtocol.encodeTelemetryEnvelope(typeTag, jsonPayload) ?: ByteArray(0)
    }
}
