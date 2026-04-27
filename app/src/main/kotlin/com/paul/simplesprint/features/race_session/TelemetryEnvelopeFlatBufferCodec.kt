package com.paul.simplesprint.features.race_session

import SprintSync.Schema.ClockResyncRequest as FbClockResyncRequest
import SprintSync.Schema.DeviceConfigUpdate as FbDeviceConfigUpdate
import SprintSync.Schema.DeviceIdentity as FbDeviceIdentity
import SprintSync.Schema.DeviceTelemetry as FbDeviceTelemetry
import SprintSync.Schema.HostControlCommand as FbHostControlCommand
import SprintSync.Schema.LapResult as FbLapResult
import SprintSync.Schema.SessionDeviceRole as FbSessionDeviceRole
import SprintSync.Schema.SessionSnapshot as FbSessionSnapshot
import SprintSync.Schema.SessionSnapshotDevice as FbSessionSnapshotDevice
import SprintSync.Schema.SessionSplitMark as FbSessionSplitMark
import SprintSync.Schema.SessionTimelineSnapshot as FbSessionTimelineSnapshot
import SprintSync.Schema.SessionTrigger as FbSessionTrigger
import SprintSync.Schema.SessionTriggerRequest as FbSessionTriggerRequest
import SprintSync.Schema.TelemetryEnvelope
import SprintSync.Schema.TelemetryPayload
import SprintSync.Schema.TriggerRefinement as FbTriggerRefinement
import com.google.flatbuffers.FlatBufferBuilder
import java.nio.ByteBuffer

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

    data class HostControlCommandEnvelope(val message: SessionHostControlCommandMessage) : DecodedTelemetryEnvelope
}

object TelemetryEnvelopeFlatBufferCodec {
    fun encodeTriggerRequest(message: SessionTriggerRequestMessage): ByteArray {
        val sourceDeviceId = message.sourceDeviceId.trim()
        if (sourceDeviceId.isEmpty()) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(256)
        val sourceDeviceIdOffset = builder.createString(sourceDeviceId)
        val payloadOffset = FbSessionTriggerRequest.createSessionTriggerRequest(
            builder,
            toSchemaRole(message.role),
            message.triggerSensorNanos,
            message.mappedHostSensorNanos ?: -1L,
            sourceDeviceIdOffset,
            message.sourceElapsedNanos,
            message.mappedAnchorElapsedNanos ?: -1L,
        )
        return finishEnvelope(builder, TelemetryPayload.SessionTriggerRequest, payloadOffset)
    }

    fun encodeTrigger(message: SessionTriggerMessage): ByteArray {
        val triggerType = message.triggerType.trim()
        if (triggerType.isEmpty()) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(128)
        val triggerTypeOffset = builder.createString(triggerType)
        val payloadOffset = FbSessionTrigger.createSessionTrigger(
            builder,
            triggerTypeOffset,
            message.splitIndex,
            message.triggerSensorNanos,
        )
        return finishEnvelope(builder, TelemetryPayload.SessionTrigger, payloadOffset)
    }

    fun encodeTimelineSnapshot(message: SessionTimelineSnapshotMessage): ByteArray {
        val builder = FlatBufferBuilder(384)
        val splitMarkOffsets = createSplitMarkOffsets(builder, message.hostSplitMarks) ?: return ByteArray(0)
        val splitMarksVectorOffset = if (splitMarkOffsets.isEmpty()) {
            0
        } else {
            FbSessionTimelineSnapshot.createHostSplitMarksVector(builder, splitMarkOffsets)
        }

        val payloadOffset = FbSessionTimelineSnapshot.createSessionTimelineSnapshot(
            builder,
            message.hostStartSensorNanos ?: -1L,
            message.hostStopSensorNanos ?: -1L,
            splitMarksVectorOffset,
            message.sentElapsedNanos,
        )
        return finishEnvelope(builder, TelemetryPayload.SessionTimelineSnapshot, payloadOffset)
    }

    fun encodeSnapshot(message: SessionSnapshotMessage): ByteArray {
        if (message.devices.isEmpty()) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(1024)
        val deviceOffsets = IntArray(message.devices.size)
        for (index in message.devices.indices) {
            val device = message.devices[index]
            val id = device.id.trim()
            val name = device.name.trim()
            if (id.isEmpty() || name.isEmpty()) {
                return ByteArray(0)
            }

            val idOffset = builder.createString(id)
            val nameOffset = builder.createString(name)
            val roleOffset = builder.createString(device.role.name.lowercase())
            val cameraFacingOffset = builder.createString(device.cameraFacing.name.lowercase())
            deviceOffsets[index] = FbSessionSnapshotDevice.createSessionSnapshotDevice(
                builder,
                idOffset,
                nameOffset,
                roleOffset,
                cameraFacingOffset,
                device.isLocal,
            )
        }

        val splitMarkOffsets = createSplitMarkOffsets(builder, message.hostSplitMarks) ?: return ByteArray(0)
        val devicesVectorOffset = FbSessionSnapshot.createDevicesVector(builder, deviceOffsets)
        val splitMarksVectorOffset = if (splitMarkOffsets.isEmpty()) {
            0
        } else {
            FbSessionSnapshot.createHostSplitMarksVector(builder, splitMarkOffsets)
        }

        val payloadOffset = FbSessionSnapshot.createSessionSnapshot(
            builder,
            builder.createString(message.stage.name.lowercase()),
            message.monitoringActive,
            devicesVectorOffset,
            message.hostStartSensorNanos ?: -1L,
            message.hostStopSensorNanos ?: -1L,
            splitMarksVectorOffset,
            createOptionalString(builder, message.runId),
            message.hostSensorMinusElapsedNanos ?: -1L,
            message.hostGpsUtcOffsetNanos ?: -1L,
            message.hostGpsFixAgeNanos ?: -1L,
            createOptionalString(builder, message.selfDeviceId),
            createOptionalString(builder, message.anchorDeviceId),
            createOptionalString(builder, message.anchorState?.name?.lowercase()),
        )
        return finishEnvelope(builder, TelemetryPayload.SessionSnapshot, payloadOffset)
    }

    fun encodeTriggerRefinement(message: SessionTriggerRefinementMessage): ByteArray {
        val runId = message.runId.trim()
        if (runId.isEmpty()) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(196)
        val runIdOffset = builder.createString(runId)
        val payloadOffset = FbTriggerRefinement.createTriggerRefinement(
            builder,
            runIdOffset,
            toSchemaRole(message.role),
            message.provisionalHostSensorNanos,
            message.refinedHostSensorNanos,
        )
        return finishEnvelope(builder, TelemetryPayload.TriggerRefinement, payloadOffset)
    }

    fun encodeDeviceConfigUpdate(message: SessionDeviceConfigUpdateMessage): ByteArray {
        val targetStableDeviceId = message.targetStableDeviceId.trim()
        if (targetStableDeviceId.isEmpty() || message.sensitivity !in 1..100) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(96)
        val targetStableDeviceIdOffset = builder.createString(targetStableDeviceId)
        val payloadOffset = FbDeviceConfigUpdate.createDeviceConfigUpdate(
            builder,
            targetStableDeviceIdOffset,
            message.sensitivity,
        )
        return finishEnvelope(builder, TelemetryPayload.DeviceConfigUpdate, payloadOffset)
    }

    fun encodeClockResyncRequest(message: SessionClockResyncRequestMessage): ByteArray {
        if (message.sampleCount !in 3..24) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(64)
        val payloadOffset = FbClockResyncRequest.createClockResyncRequest(builder, message.sampleCount)
        return finishEnvelope(builder, TelemetryPayload.ClockResyncRequest, payloadOffset)
    }

    fun encodeHostControlCommand(message: SessionHostControlCommandMessage): ByteArray {
        val action = message.action.name.lowercase()
        if (action.isEmpty()) {
            return ByteArray(0)
        }
        if (message.action == SessionHostControlAction.TIMER_SCALE_DELTA && message.value !in setOf(-1L, 1L)) {
            return ByteArray(0)
        }
        if (message.action == SessionHostControlAction.SET_TIMER_LIMIT_MS && (message.value == null || message.value <= 0)) {
            return ByteArray(0)
        }
        if (
            message.action != SessionHostControlAction.TIMER_SCALE_DELTA &&
            message.action != SessionHostControlAction.SET_TIMER_LIMIT_MS &&
            message.value != null
        ) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(128)
        val actionOffset = builder.createString(action)
        val payloadOffset = FbHostControlCommand.createHostControlCommand(
            builder,
            actionOffset,
            message.value?.toInt() ?: 0,
            message.value != null,
        )
        return finishEnvelope(builder, TelemetryPayload.HostControlCommand, payloadOffset)
    }

    fun encodeDeviceIdentity(message: SessionDeviceIdentityMessage): ByteArray {
        val stableDeviceId = message.stableDeviceId.trim()
        val deviceName = message.deviceName.trim()
        if (stableDeviceId.isEmpty() || deviceName.isEmpty()) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(128)
        val stableDeviceIdOffset = builder.createString(stableDeviceId)
        val deviceNameOffset = builder.createString(deviceName)
        val payloadOffset = FbDeviceIdentity.createDeviceIdentity(
            builder,
            stableDeviceIdOffset,
            deviceNameOffset,
        )
        return finishEnvelope(builder, TelemetryPayload.DeviceIdentity, payloadOffset)
    }

    fun encodeDeviceTelemetry(message: SessionDeviceTelemetryMessage): ByteArray {
        val stableDeviceId = message.stableDeviceId.trim()
        if (stableDeviceId.isEmpty()) {
            return ByteArray(0)
        }

        if (message.sensitivity !in 1..100) {
            return ByteArray(0)
        }
        if (message.latencyMs != null && message.latencyMs < 0) {
            return ByteArray(0)
        }
        if ((message.analysisWidth == null) != (message.analysisHeight == null)) {
            return ByteArray(0)
        }
        if (
            message.analysisWidth != null &&
            (message.analysisWidth <= 0 || message.analysisHeight == null || message.analysisHeight <= 0)
        ) {
            return ByteArray(0)
        }
        if (message.timestampMillis <= 0L) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(256)
        val stableDeviceIdOffset = builder.createString(stableDeviceId)
        val payloadOffset = FbDeviceTelemetry.createDeviceTelemetry(
            builder,
            stableDeviceIdOffset,
            toSchemaRole(message.role),
            message.sensitivity,
            message.latencyMs ?: -1,
            message.clockSynced,
            message.analysisWidth ?: -1,
            message.analysisHeight ?: -1,
            message.timestampMillis,
        )
        // Note: The schema for DeviceTelemetry doesn't seem to have deviceName yet,
        // but we might need to update the schema or use an offset if it was added.
        // Checking schema: table DeviceTelemetry { stableDeviceId: string; role: SessionDeviceRole = UNASSIGNED; sensitivity: int; latencyMs: int = -1; clockSynced: bool; analysisWidth: int = -1; analysisHeight: int = -1; timestampMillis: long; }
        // It's NOT in the schema. I should probably add it to the schema or ignore for now if I can't change schema.
        // Wait, the error said "No value passed for parameter 'deviceName'".
        // Ah, the error is in the DECODE part for SessionDeviceTelemetryMessage constructor.
        return finishEnvelope(builder, TelemetryPayload.DeviceTelemetry, payloadOffset)
    }

    fun encodeLapResult(message: SessionLapResultMessage): ByteArray {
        val senderDeviceName = message.senderDeviceName.trim()
        if (senderDeviceName.isEmpty() || message.stoppedSensorNanos <= message.startedSensorNanos) {
            return ByteArray(0)
        }

        val builder = FlatBufferBuilder(128)
        val senderDeviceNameOffset = builder.createString(senderDeviceName)
        val payloadOffset = FbLapResult.createLapResult(
            builder,
            senderDeviceNameOffset,
            message.startedSensorNanos,
            message.stoppedSensorNanos,
        )
        return finishEnvelope(builder, TelemetryPayload.LapResult, payloadOffset)
    }

    fun decode(payloadBytes: ByteArray): DecodedTelemetryEnvelope? {
        return runCatching {
            val envelope = TelemetryEnvelope.getRootAsTelemetryEnvelope(ByteBuffer.wrap(payloadBytes))

            when (envelope.payloadType()) {
                TelemetryPayload.SessionTriggerRequest -> {
                    val payload = envelope.payload(FbSessionTriggerRequest()) as? FbSessionTriggerRequest ?: return null
                    val sourceDeviceId = payload.sourceDeviceId()?.trim().orEmpty()
                    if (sourceDeviceId.isEmpty()) {
                        return null
                    }
                    DecodedTelemetryEnvelope.TriggerRequest(
                        SessionTriggerRequestMessage(
                            role = fromSchemaRole(payload.role()),
                            triggerSensorNanos = payload.triggerSensorNanos(),
                            mappedHostSensorNanos = payload.mappedHostSensorNanos().toOptionalLong(),
                            sourceDeviceId = sourceDeviceId,
                            sourceElapsedNanos = payload.sourceElapsedNanos(),
                            mappedAnchorElapsedNanos = payload.mappedAnchorElapsedNanos().toOptionalLong(),
                        ),
                    )
                }

                TelemetryPayload.SessionTrigger -> {
                    val payload = envelope.payload(FbSessionTrigger()) as? FbSessionTrigger ?: return null
                    val triggerType = payload.triggerType()?.trim().orEmpty()
                    if (triggerType.isEmpty()) {
                        return null
                    }
                    DecodedTelemetryEnvelope.Trigger(
                        SessionTriggerMessage(
                            triggerType = triggerType,
                            splitIndex = payload.splitIndex(),
                            triggerSensorNanos = payload.triggerSensorNanos(),
                        ),
                    )
                }

                TelemetryPayload.SessionTimelineSnapshot -> {
                    val payload = envelope.payload(FbSessionTimelineSnapshot()) as? FbSessionTimelineSnapshot ?: return null
                    DecodedTelemetryEnvelope.TimelineSnapshot(
                        SessionTimelineSnapshotMessage(
                            hostStartSensorNanos = payload.hostStartSensorNanos().toOptionalLong(),
                            hostStopSensorNanos = payload.hostStopSensorNanos().toOptionalLong(),
                            hostSplitMarks = decodeSplitMarks(
                                payload.hostSplitMarksLength(),
                                payload::hostSplitMarks,
                            ),
                            sentElapsedNanos = payload.sentElapsedNanos(),
                        ),
                    )
                }

                TelemetryPayload.SessionSnapshot -> {
                    val payload = envelope.payload(FbSessionSnapshot()) as? FbSessionSnapshot ?: return null
                    val stage = sessionStageFromName(payload.stage() ?: "lobby") ?: return null

                    val devices = mutableListOf<SessionDevice>()
                    for (index in 0 until payload.devicesLength()) {
                        val encodedDevice = payload.devices(index) ?: continue
                        val id = encodedDevice.id()?.trim().orEmpty()
                        val name = (encodedDevice.name() ?: "Unknown").trim()
                        if (id.isEmpty() || name.isEmpty()) {
                            continue
                        }
                        devices += SessionDevice(
                            id = id,
                            name = name,
                            role = decodeSnapshotDeviceRole(encodedDevice.role()),
                            cameraFacing = decodeSnapshotCameraFacing(encodedDevice.cameraFacing()),
                            isLocal = encodedDevice.isLocal(),
                        )
                    }
                    if (devices.isEmpty()) {
                        return null
                    }

                    DecodedTelemetryEnvelope.Snapshot(
                        SessionSnapshotMessage(
                            stage = stage,
                            monitoringActive = payload.monitoringActive(),
                            devices = devices,
                            hostStartSensorNanos = payload.hostStartSensorNanos().toOptionalLong(),
                            hostStopSensorNanos = payload.hostStopSensorNanos().toOptionalLong(),
                            hostSplitMarks = decodeSplitMarks(
                                payload.hostSplitMarksLength(),
                                payload::hostSplitMarks,
                            ),
                            runId = payload.runId()?.ifBlank { null },
                            hostSensorMinusElapsedNanos = payload.hostSensorMinusElapsedNanos().toOptionalLong(),
                            hostGpsUtcOffsetNanos = payload.hostGpsUtcOffsetNanos().toOptionalLong(),
                            hostGpsFixAgeNanos = payload.hostGpsFixAgeNanos().toOptionalLong(),
                            selfDeviceId = payload.selfDeviceId()?.ifBlank { null },
                            anchorDeviceId = payload.anchorDeviceId()?.ifBlank { null },
                            anchorState = sessionAnchorStateFromName(payload.anchorState()?.ifBlank { null }),
                        ),
                    )
                }

                TelemetryPayload.TriggerRefinement -> {
                    val payload = envelope.payload(FbTriggerRefinement()) as? FbTriggerRefinement ?: return null
                    val runId = payload.runId()?.trim().orEmpty()
                    if (runId.isEmpty()) {
                        return null
                    }
                    DecodedTelemetryEnvelope.TriggerRefinementEnvelope(
                        SessionTriggerRefinementMessage(
                            runId = runId,
                            role = fromSchemaRole(payload.role()),
                            provisionalHostSensorNanos = payload.provisionalHostSensorNanos(),
                            refinedHostSensorNanos = payload.refinedHostSensorNanos(),
                        ),
                    )
                }

                TelemetryPayload.DeviceConfigUpdate -> {
                    val payload = envelope.payload(FbDeviceConfigUpdate()) as? FbDeviceConfigUpdate ?: return null
                    val targetStableDeviceId = payload.targetStableDeviceId()?.trim().orEmpty()
                    if (targetStableDeviceId.isEmpty() || payload.sensitivity() !in 1..100) {
                        return null
                    }
                    DecodedTelemetryEnvelope.ConfigUpdate(
                        SessionDeviceConfigUpdateMessage(
                            targetStableDeviceId = targetStableDeviceId,
                            sensitivity = payload.sensitivity(),
                        ),
                    )
                }

                TelemetryPayload.ClockResyncRequest -> {
                    val payload = envelope.payload(FbClockResyncRequest()) as? FbClockResyncRequest ?: return null
                    if (payload.sampleCount() !in 3..24) {
                        return null
                    }
                    DecodedTelemetryEnvelope.ClockResync(
                        SessionClockResyncRequestMessage(
                            reason = "flatbuffer_sync", // Schema missing reason, using default
                            sampleCount = payload.sampleCount(),
                        ),
                    )
                }

                TelemetryPayload.DeviceIdentity -> {
                    val payload = envelope.payload(FbDeviceIdentity()) as? FbDeviceIdentity ?: return null
                    val stableDeviceId = payload.stableDeviceId()?.trim().orEmpty()
                    val deviceName = payload.deviceName()?.trim().orEmpty()
                    if (stableDeviceId.isEmpty() || deviceName.isEmpty()) {
                        return null
                    }
                    DecodedTelemetryEnvelope.Identity(
                        SessionDeviceIdentityMessage(
                            stableDeviceId = stableDeviceId,
                            deviceName = deviceName,
                        ),
                    )
                }

                TelemetryPayload.DeviceTelemetry -> {
                    val payload = envelope.payload(FbDeviceTelemetry()) as? FbDeviceTelemetry ?: return null
                    val stableDeviceId = payload.stableDeviceId()?.trim().orEmpty()
                    val sensitivity = payload.sensitivity()
                    val latencyMs = payload.latencyMs().toOptionalInt()
                    val analysisWidth = payload.analysisWidth().toOptionalInt()
                    val analysisHeight = payload.analysisHeight().toOptionalInt()

                    if (stableDeviceId.isEmpty() || sensitivity !in 1..100) {
                        return null
                    }
                    if ((analysisWidth == null) != (analysisHeight == null)) {
                        return null
                    }
                    if (
                        analysisWidth != null &&
                        (analysisWidth <= 0 || analysisHeight == null || analysisHeight <= 0)
                    ) {
                        return null
                    }
                    if (latencyMs != null && latencyMs < 0) {
                        return null
                    }
                    if (payload.timestampMillis() <= 0L) {
                        return null
                    }

                    DecodedTelemetryEnvelope.DeviceTelemetryEnvelope(
                        SessionDeviceTelemetryMessage(
                            stableDeviceId = stableDeviceId,
                            deviceName = "Remote Device", // Schema missing deviceName, using placeholder
                            role = fromSchemaRole(payload.role()),
                            sensitivity = sensitivity,
                            latencyMs = latencyMs,
                            clockSynced = payload.clockSynced(),
                            analysisWidth = analysisWidth,
                            analysisHeight = analysisHeight,
                            timestampMillis = payload.timestampMillis(),
                        ),
                    )
                }

                TelemetryPayload.LapResult -> {
                    val payload = envelope.payload(FbLapResult()) as? FbLapResult ?: return null
                    val senderDeviceName = payload.senderDeviceName()?.trim().orEmpty()
                    if (senderDeviceName.isEmpty() || payload.stoppedSensorNanos() <= payload.startedSensorNanos()) {
                        return null
                    }
                    DecodedTelemetryEnvelope.LapResultEnvelope(
                        SessionLapResultMessage(
                            senderDeviceName = senderDeviceName,
                            startedSensorNanos = payload.startedSensorNanos(),
                            stoppedSensorNanos = payload.stoppedSensorNanos(),
                        ),
                    )
                }

                TelemetryPayload.HostControlCommand -> {
                    val payload = envelope.payload(FbHostControlCommand()) as? FbHostControlCommand ?: return null
                    val action = sessionHostControlActionFromName(payload.action()?.trim()) ?: return null
                    val intValue = if (payload.hasIntValue()) payload.intValue().toLong() else null
                    if (action == SessionHostControlAction.TIMER_SCALE_DELTA && intValue !in setOf(-1L, 1L)) {
                        return null
                    }
                    if (action == SessionHostControlAction.SET_TIMER_LIMIT_MS && (intValue == null || intValue <= 0L)) {
                        return null
                    }
                    if (
                        action != SessionHostControlAction.TIMER_SCALE_DELTA &&
                        action != SessionHostControlAction.SET_TIMER_LIMIT_MS &&
                        intValue != null
                    ) {
                        return null
                    }
                    val message = SessionHostControlCommandMessage(
                        action = action,
                        value = intValue,
                    )
                    DecodedTelemetryEnvelope.HostControlCommandEnvelope(message)
                }

                else -> null
            }
        }.getOrNull()
    }

    private fun finishEnvelope(builder: FlatBufferBuilder, payloadType: Byte, payloadOffset: Int): ByteArray {
        val envelopeOffset = TelemetryEnvelope.createTelemetryEnvelope(
            builder,
            payloadType,
            payloadOffset,
        )
        TelemetryEnvelope.finishTelemetryEnvelopeBuffer(builder, envelopeOffset)
        return builder.sizedByteArray()
    }

    private fun createOptionalString(builder: FlatBufferBuilder, value: String?): Int {
        val text = value?.trim().orEmpty()
        return if (text.isEmpty()) 0 else builder.createString(text)
    }

    private fun createSplitMarkOffsets(builder: FlatBufferBuilder, marks: List<SessionSplitMark>): IntArray? {
        val offsets = IntArray(marks.size)
        for (index in marks.indices) {
            val mark = marks[index]
            if (mark.role !in explicitSplitRoles()) {
                return null
            }
            offsets[index] = FbSessionSplitMark.createSessionSplitMark(
                builder,
                toSchemaRole(mark.role),
                mark.hostSensorNanos,
            )
        }
        return offsets
    }

    private fun decodeSplitMarks(length: Int, accessor: (Int) -> FbSessionSplitMark?): List<SessionSplitMark> {
        val marks = mutableListOf<SessionSplitMark>()
        for (index in 0 until length) {
            val encodedMark = accessor(index) ?: continue
            val role = fromSchemaRole(encodedMark.role())
            if (role !in explicitSplitRoles()) {
                continue
            }
            marks += SessionSplitMark(
                role = role,
                hostSensorNanos = encodedMark.hostSensorNanos(),
                splitIndex = splitIndexForRole(role) ?: -1,
            )
        }
        return marks
    }

    private fun toSchemaRole(role: SessionDeviceRole): Byte {
        return when (role) {
            SessionDeviceRole.UNASSIGNED -> FbSessionDeviceRole.UNASSIGNED
            SessionDeviceRole.START -> FbSessionDeviceRole.START
            SessionDeviceRole.SPLIT1 -> FbSessionDeviceRole.SPLIT1
            SessionDeviceRole.SPLIT2 -> FbSessionDeviceRole.SPLIT2
            SessionDeviceRole.SPLIT3 -> FbSessionDeviceRole.SPLIT3
            SessionDeviceRole.SPLIT4 -> FbSessionDeviceRole.SPLIT4
            SessionDeviceRole.STOP -> FbSessionDeviceRole.STOP
            SessionDeviceRole.DISPLAY -> FbSessionDeviceRole.DISPLAY
            SessionDeviceRole.CONTROLLER -> FbSessionDeviceRole.CONTROLLER
        }
    }

    private fun fromSchemaRole(role: Byte): SessionDeviceRole {
        return when (role) {
            FbSessionDeviceRole.START -> SessionDeviceRole.START
            FbSessionDeviceRole.SPLIT1 -> SessionDeviceRole.SPLIT1
            FbSessionDeviceRole.SPLIT2 -> SessionDeviceRole.SPLIT2
            FbSessionDeviceRole.SPLIT3 -> SessionDeviceRole.SPLIT3
            FbSessionDeviceRole.SPLIT4 -> SessionDeviceRole.SPLIT4
            FbSessionDeviceRole.STOP -> SessionDeviceRole.STOP
            FbSessionDeviceRole.DISPLAY -> SessionDeviceRole.DISPLAY
            FbSessionDeviceRole.CONTROLLER -> SessionDeviceRole.CONTROLLER
            else -> SessionDeviceRole.UNASSIGNED
        }
    }

    private fun decodeSnapshotDeviceRole(role: String?): SessionDeviceRole {
        return when (role?.trim()?.lowercase()) {
            "start" -> SessionDeviceRole.START
            "split", "split1" -> SessionDeviceRole.SPLIT1
            "split2" -> SessionDeviceRole.SPLIT2
            "split3" -> SessionDeviceRole.SPLIT3
            "split4" -> SessionDeviceRole.SPLIT4
            "stop" -> SessionDeviceRole.STOP
            "display" -> SessionDeviceRole.DISPLAY
            "controller" -> SessionDeviceRole.CONTROLLER
            else -> SessionDeviceRole.UNASSIGNED
        }
    }

    private fun decodeSnapshotCameraFacing(facing: String?): SessionCameraFacing {
        return if (facing?.trim()?.equals("front", ignoreCase = true) == true) {
            SessionCameraFacing.FRONT
        } else {
            SessionCameraFacing.REAR
        }
    }

    private fun Long.toOptionalLong(): Long? = takeIf { it >= 0L }

    private fun Int.toOptionalInt(): Int? = takeIf { it >= 0 }
}
