package com.paul.simplesprint.features.race_session

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class SessionStage {
    SETUP,
    LOBBY,
    MONITORING,
}

enum class SessionOperatingMode {
    NETWORK_RACE,
    SINGLE_DEVICE,
    DISPLAY_HOST,
}

enum class SessionNetworkRole {
    NONE,
    HOST,
    CLIENT,
}

enum class SessionDeviceRole {
    UNASSIGNED,
    START,
    SPLIT1,
    SPLIT2,
    SPLIT3,
    SPLIT4,
    STOP,
    DISPLAY,
    CONTROLLER,
}

enum class SessionHostControlAction {
    START_MONITORING,
    STOP_MONITORING,
    RESET_RUN,
    TIMER_SCALE_DELTA,
    SET_TIMER_LIMIT_MS,
}

enum class SessionCameraFacing {
    REAR,
    FRONT,
}

enum class SessionAnchorState {
    READY,
    ACTIVE,
    LOST,
}

enum class SessionClockLockReason {
    OK,
    NO_ANCHOR,
    LOCK_STALE,
    ANCHOR_LOST,
}

data class SessionDevice(
    val id: String,
    val name: String,
    val role: SessionDeviceRole,
    val cameraFacing: SessionCameraFacing = SessionCameraFacing.FRONT,
    val isLocal: Boolean,
    val stableId: String? = null,
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
            .put("id", id)
            .put("name", name)
            .put("role", role.name.lowercase())
            .put("cameraFacing", cameraFacing.name.lowercase())
            .put("isLocal", isLocal)
        stableId?.let { obj.put("stableId", it) }
        return obj
    }

    companion object {
        fun fromJsonObject(decoded: JSONObject): SessionDevice? {
            val id = decoded.optString("id", "").trim()
            val name = decoded.optString("name", "").trim()
            val role = sessionDeviceRoleFromName(decoded.readOptionalString("role"))
            val cameraFacing = sessionCameraFacingFromName(decoded.readOptionalString("cameraFacing"))
                ?: SessionCameraFacing.FRONT
            val stableId = decoded.readOptionalString("stableId")
            if (id.isEmpty() || name.isEmpty() || role == null) {
                return null
            }
            return SessionDevice(
                id = id,
                name = name,
                role = role,
                cameraFacing = cameraFacing,
                isLocal = decoded.optBoolean("isLocal", false),
                stableId = stableId,
            )
        }
    }
}

data class SessionSnapshotMessage(
    val stage: SessionStage,
    val monitoringActive: Boolean,
    val devices: List<SessionDevice>,
    val hostStartSensorNanos: Long?,
    val hostStopSensorNanos: Long?,
    val hostSplitMarks: List<SessionSplitMark> = emptyList(),
    val runId: String?,
    val hostSensorMinusElapsedNanos: Long?,
    val hostGpsUtcOffsetNanos: Long?,
    val hostGpsFixAgeNanos: Long?,
    val selfDeviceId: String?,
    val anchorDeviceId: String?,
    val anchorState: SessionAnchorState?,
) {
    fun toJsonString(): String {
        val devicesArray = JSONArray()
        devices.forEach { devicesArray.put(it.toJsonObject()) }
        val timeline = JSONObject()
            .put("hostStartSensorNanos", hostStartSensorNanos ?: JSONObject.NULL)
            .put("hostStopSensorNanos", hostStopSensorNanos ?: JSONObject.NULL)
            .put("hostSplitMarks", hostSplitMarks.toJsonObjectArray())
            .put("hostSplitSensorNanos", hostSplitMarks.map { it.hostSensorNanos }.toJsonArray())
        return JSONObject()
            .put("type", TYPE)
            .put("stage", stage.name.lowercase())
            .put("monitoringActive", monitoringActive)
            .put("devices", devicesArray)
            .put("hostStartSensorNanos", hostStartSensorNanos ?: JSONObject.NULL)
            .put("hostStopSensorNanos", hostStopSensorNanos ?: JSONObject.NULL)
            .put("timeline", timeline)
            .put("runId", runId ?: JSONObject.NULL)
            .put("hostSensorMinusElapsedNanos", hostSensorMinusElapsedNanos ?: JSONObject.NULL)
            .put("hostGpsUtcOffsetNanos", hostGpsUtcOffsetNanos ?: JSONObject.NULL)
            .put("hostGpsFixAgeNanos", hostGpsFixAgeNanos ?: JSONObject.NULL)
            .put("selfDeviceId", selfDeviceId ?: JSONObject.NULL)
            .put("anchorDeviceId", anchorDeviceId ?: JSONObject.NULL)
            .put("anchorState", anchorState?.name?.lowercase() ?: JSONObject.NULL)
            .toString()
    }

    companion object {
        const val TYPE = "snapshot"

        fun tryParse(json: String): SessionSnapshotMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                val stage = sessionStageFromName(decoded.readOptionalString("stage")) ?: SessionStage.SETUP
                val monitoringActive = decoded.optBoolean("monitoringActive", false)
                val devicesArray = decoded.optJSONArray("devices")
                val devices = mutableListOf<SessionDevice>()
                if (devicesArray != null) {
                    for (i in 0 until devicesArray.length()) {
                        SessionDevice.fromJsonObject(devicesArray.getJSONObject(i))?.let { devices.add(it) }
                    }
                }

                val timeline = decoded.optJSONObject("timeline")
                val hostStartSensorNanos = timeline?.optLong("hostStartSensorNanos", -1L)?.takeIf { it > -1 }
                    ?: decoded.optLong("hostStartSensorNanos", -1L).takeIf { it > -1 }
                val hostStopSensorNanos = timeline?.optLong("hostStopSensorNanos", -1L)?.takeIf { it > -1 }
                    ?: decoded.optLong("hostStopSensorNanos", -1L).takeIf { it > -1 }
                val hostSplitMarks = timeline?.optJSONArray("hostSplitMarks")?.toSessionSplitMarkList() ?: emptyList()

                return SessionSnapshotMessage(
                    stage = stage,
                    monitoringActive = monitoringActive,
                    devices = devices,
                    hostStartSensorNanos = hostStartSensorNanos,
                    hostStopSensorNanos = hostStopSensorNanos,
                    hostSplitMarks = hostSplitMarks,
                    runId = decoded.readOptionalString("runId"),
                    hostSensorMinusElapsedNanos = decoded.optLong("hostSensorMinusElapsedNanos", -1L).takeIf { it > -1 },
                    hostGpsUtcOffsetNanos = decoded.optLong("hostGpsUtcOffsetNanos", -1L).takeIf { it > -1 },
                    hostGpsFixAgeNanos = decoded.optLong("hostGpsFixAgeNanos", -1L).takeIf { it > -1 },
                    selfDeviceId = decoded.readOptionalString("selfDeviceId"),
                    anchorDeviceId = decoded.readOptionalString("anchorDeviceId"),
                    anchorState = sessionAnchorStateFromName(decoded.readOptionalString("anchorState")),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

fun List<SessionSplitMark>.toJsonObjectArray(): JSONArray {
    val array = JSONArray()
    forEach { array.put(it.toJsonObject()) }
    return array
}

fun JSONArray.toSessionSplitMarkList(): List<SessionSplitMark> {
    val list = mutableListOf<SessionSplitMark>()
    for (i in 0 until length()) {
        val obj = getJSONObject(i)
        list.add(
            SessionSplitMark(
                hostSensorNanos = obj.getLong("hostSensorNanos"),
                role = sessionDeviceRoleFromName(obj.optString("role")) ?: SessionDeviceRole.UNASSIGNED,
                splitIndex = obj.getInt("splitIndex"),
            ),
        )
    }
    return list
}

data class SessionSplitMark(
    val hostSensorNanos: Long,
    val role: SessionDeviceRole,
    val splitIndex: Int,
) {
    fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("hostSensorNanos", hostSensorNanos)
            .put("role", role.name.lowercase())
            .put("splitIndex", splitIndex)
    }
}

data class SessionTriggerMessage(
    val triggerType: String,
    val splitIndex: Int,
    val triggerSensorNanos: Long,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("triggerType", triggerType)
            .put("splitIndex", splitIndex)
            .put("triggerSensorNanos", triggerSensorNanos)
            .toString()
    }

    companion object {
        const val TYPE = "trigger"
        fun tryParse(json: String): SessionTriggerMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                return SessionTriggerMessage(
                    triggerType = decoded.getString("triggerType"),
                    splitIndex = decoded.getInt("splitIndex"),
                    triggerSensorNanos = decoded.getLong("triggerSensorNanos"),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionTriggerRequestMessage(
    val role: SessionDeviceRole,
    val triggerSensorNanos: Long,
    val mappedHostSensorNanos: Long?,
    val sourceDeviceId: String,
    val sourceElapsedNanos: Long,
    val mappedAnchorElapsedNanos: Long?,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("role", role.name.lowercase())
            .put("triggerSensorNanos", triggerSensorNanos)
            .put("mappedHostSensorNanos", mappedHostSensorNanos ?: JSONObject.NULL)
            .put("sourceDeviceId", sourceDeviceId)
            .put("sourceElapsedNanos", sourceElapsedNanos)
            .put("mappedAnchorElapsedNanos", mappedAnchorElapsedNanos ?: JSONObject.NULL)
            .toString()
    }

    companion object {
        const val TYPE = "trigger_request"
        fun tryParse(json: String): SessionTriggerRequestMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                val role = sessionDeviceRoleFromName(decoded.readOptionalString("role")) ?: return null
                return SessionTriggerRequestMessage(
                    role = role,
                    triggerSensorNanos = decoded.getLong("triggerSensorNanos"),
                    mappedHostSensorNanos = decoded.optLong("mappedHostSensorNanos", -1L).takeIf { it > -1 },
                    sourceDeviceId = decoded.getString("sourceDeviceId"),
                    sourceElapsedNanos = decoded.getLong("sourceElapsedNanos"),
                    mappedAnchorElapsedNanos = decoded.optLong("mappedAnchorElapsedNanos", -1L).takeIf { it > -1 },
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionDeviceIdentityMessage(
    val stableDeviceId: String,
    val deviceName: String,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("stableDeviceId", stableDeviceId)
            .put("deviceName", deviceName)
            .toString()
    }

    companion object {
        const val TYPE = "identity"
        fun tryParse(json: String): SessionDeviceIdentityMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                return SessionDeviceIdentityMessage(
                    stableDeviceId = decoded.getString("stableDeviceId"),
                    deviceName = decoded.getString("deviceName"),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionDeviceTelemetryMessage(
    val stableDeviceId: String,
    val deviceName: String,
    val role: SessionDeviceRole,
    val sensitivity: Int,
    val latencyMs: Int?,
    val clockSynced: Boolean,
    val analysisWidth: Int? = null,
    val analysisHeight: Int? = null,
    val timestampMillis: Long,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("stableDeviceId", stableDeviceId)
            .put("deviceName", deviceName)
            .put("role", role.name.lowercase())
            .put("sensitivity", sensitivity)
            .put("latencyMs", latencyMs ?: JSONObject.NULL)
            .put("clockSynced", clockSynced)
            .put("analysisWidth", analysisWidth ?: JSONObject.NULL)
            .put("analysisHeight", analysisHeight ?: JSONObject.NULL)
            .put("timestampMillis", timestampMillis)
            .toString()
    }

    companion object {
        const val TYPE = "telemetry"
        fun tryParse(json: String): SessionDeviceTelemetryMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                val role = sessionDeviceRoleFromName(decoded.readOptionalString("role")) ?: SessionDeviceRole.UNASSIGNED
                return SessionDeviceTelemetryMessage(
                    stableDeviceId = decoded.getString("stableDeviceId"),
                    deviceName = decoded.getString("deviceName"),
                    role = role,
                    sensitivity = decoded.getInt("sensitivity"),
                    latencyMs = decoded.optInt("latencyMs", -1).takeIf { it > -1 },
                    clockSynced = decoded.getBoolean("clockSynced"),
                    analysisWidth = decoded.optInt("analysisWidth", -1).takeIf { it > -1 },
                    analysisHeight = decoded.optInt("analysisHeight", -1).takeIf { it > -1 },
                    timestampMillis = decoded.optLong("timestampMillis", 0L),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionDeviceConfigUpdateMessage(
    val targetStableDeviceId: String,
    val sensitivity: Int,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("targetStableDeviceId", targetStableDeviceId)
            .put("sensitivity", sensitivity)
            .toString()
    }

    companion object {
        const val TYPE = "config_update"
        fun tryParse(json: String): SessionDeviceConfigUpdateMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                return SessionDeviceConfigUpdateMessage(
                    targetStableDeviceId = decoded.getString("targetStableDeviceId"),
                    sensitivity = decoded.getInt("sensitivity"),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionHostControlCommandMessage(
    val action: SessionHostControlAction,
    val value: Long? = null,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("action", action.name.lowercase())
            .put("value", value ?: JSONObject.NULL)
            .toString()
    }

    companion object {
        const val TYPE = "host_control"
        fun tryParse(json: String): SessionHostControlCommandMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                val action = sessionHostControlActionFromName(decoded.readOptionalString("action")) ?: return null
                return SessionHostControlCommandMessage(
                    action = action,
                    value = decoded.optLong("value", -1L).takeIf { it > -1 },
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionClockResyncRequestMessage(
    val reason: String,
    val sampleCount: Int,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("reason", reason)
            .put("sampleCount", sampleCount)
            .toString()
    }

    companion object {
        const val TYPE = "clock_resync"
        fun tryParse(json: String): SessionClockResyncRequestMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                return SessionClockResyncRequestMessage(
                    reason = decoded.getString("reason"),
                    sampleCount = decoded.getInt("sampleCount"),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionTimelineSnapshotMessage(
    val hostStartSensorNanos: Long?,
    val hostStopSensorNanos: Long?,
    val hostSplitMarks: List<SessionSplitMark> = emptyList(),
    val sentElapsedNanos: Long,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("hostStartSensorNanos", hostStartSensorNanos ?: JSONObject.NULL)
            .put("hostStopSensorNanos", hostStopSensorNanos ?: JSONObject.NULL)
            .put("hostSplitMarks", hostSplitMarks.toJsonObjectArray())
            .put("sentElapsedNanos", sentElapsedNanos)
            .toString()
    }

    companion object {
        const val TYPE = "timeline_snapshot"
        fun tryParse(json: String): SessionTimelineSnapshotMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                return SessionTimelineSnapshotMessage(
                    hostStartSensorNanos = decoded.optLong("hostStartSensorNanos", -1L).takeIf { it > -1 },
                    hostStopSensorNanos = decoded.optLong("hostStopSensorNanos", -1L).takeIf { it > -1 },
                    hostSplitMarks = decoded.optJSONArray("hostSplitMarks")?.toSessionSplitMarkList() ?: emptyList(),
                    sentElapsedNanos = decoded.optLong("sentElapsedNanos", 0L),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

fun sessionStageFromName(name: String?): SessionStage? {
    return SessionStage.values().firstOrNull { it.name.lowercase() == name?.lowercase() }
}

fun sessionDeviceRoleFromName(name: String?): SessionDeviceRole? {
    return SessionDeviceRole.values().firstOrNull { it.name.lowercase() == name?.lowercase() }
}

fun sessionAnchorStateFromName(name: String?): SessionAnchorState? {
    return SessionAnchorState.values().firstOrNull { it.name.lowercase() == name?.lowercase() }
}

fun sessionHostControlActionFromName(name: String?): SessionHostControlAction? {
    return SessionHostControlAction.values().firstOrNull { it.name.lowercase() == name?.lowercase() }
}

fun sessionCameraFacingFromName(name: String?): SessionCameraFacing? {
    return SessionCameraFacing.values().firstOrNull { it.name.lowercase() == name?.lowercase() }
}

fun SessionDeviceRole.isSplitCheckpointRole(): Boolean = this in explicitSplitRoles()

fun explicitSplitRoles(): List<SessionDeviceRole> = listOf(
    SessionDeviceRole.SPLIT1,
    SessionDeviceRole.SPLIT2,
    SessionDeviceRole.SPLIT3,
    SessionDeviceRole.SPLIT4,
)

fun splitIndexForRole(role: SessionDeviceRole): Int? = when (role) {
    SessionDeviceRole.SPLIT1 -> 0
    SessionDeviceRole.SPLIT2 -> 1
    SessionDeviceRole.SPLIT3 -> 2
    SessionDeviceRole.SPLIT4 -> 3
    else -> null
}

fun splitRoleFromIndex(index: Int): SessionDeviceRole = when (index) {
    0 -> SessionDeviceRole.SPLIT1
    1 -> SessionDeviceRole.SPLIT2
    2 -> SessionDeviceRole.SPLIT3
    3 -> SessionDeviceRole.SPLIT4
    else -> SessionDeviceRole.UNASSIGNED
}

fun sessionDeviceRoleLabel(role: SessionDeviceRole): String = when (role) {
    SessionDeviceRole.UNASSIGNED -> "Unassigned"
    SessionDeviceRole.START -> "Start"
    SessionDeviceRole.SPLIT1 -> "Split 1"
    SessionDeviceRole.SPLIT2 -> "Split 2"
    SessionDeviceRole.SPLIT3 -> "Split 3"
    SessionDeviceRole.SPLIT4 -> "Split 4"
    SessionDeviceRole.STOP -> "Stop"
    SessionDeviceRole.DISPLAY -> "Display"
    SessionDeviceRole.CONTROLLER -> "Controller"
}

fun sessionCameraFacingLabel(facing: SessionCameraFacing): String = when (facing) {
    SessionCameraFacing.REAR -> "Rear"
    SessionCameraFacing.FRONT -> "Front"
}

data class SessionLapResultMessage(
    val senderDeviceName: String,
    val startedSensorNanos: Long,
    val stoppedSensorNanos: Long,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("senderDeviceName", senderDeviceName)
            .put("startedSensorNanos", startedSensorNanos)
            .put("stoppedSensorNanos", stoppedSensorNanos)
            .toString()
    }

    companion object {
        const val TYPE = "lap_result"
        fun tryParse(json: String): SessionLapResultMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                return SessionLapResultMessage(
                    senderDeviceName = decoded.getString("senderDeviceName"),
                    startedSensorNanos = decoded.getLong("startedSensorNanos"),
                    stoppedSensorNanos = decoded.getLong("stoppedSensorNanos"),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

data class SessionTriggerRefinementMessage(
    val runId: String,
    val role: SessionDeviceRole,
    val provisionalHostSensorNanos: Long,
    val refinedHostSensorNanos: Long,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("type", TYPE)
            .put("runId", runId)
            .put("role", role.name.lowercase())
            .put("provisionalHostSensorNanos", provisionalHostSensorNanos)
            .put("refinedHostSensorNanos", refinedHostSensorNanos)
            .toString()
    }

    companion object {
        const val TYPE = "trigger_refinement"
        fun tryParse(json: String): SessionTriggerRefinementMessage? {
            try {
                val decoded = JSONObject(json)
                if (decoded.optString("type") != TYPE) return null
                val role = sessionDeviceRoleFromName(decoded.readOptionalString("role")) ?: return null
                return SessionTriggerRefinementMessage(
                    runId = decoded.getString("runId"),
                    role = role,
                    provisionalHostSensorNanos = decoded.getLong("provisionalHostSensorNanos"),
                    refinedHostSensorNanos = decoded.getLong("refinedHostSensorNanos"),
                )
            } catch (e: JSONException) {
                return null
            }
        }
    }
}

private fun JSONObject.readOptionalString(key: String): String? {
    return if (has(key) && !isNull(key)) optString(key) else null
}

private fun List<Long>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { array.put(it) }
    return array
}
