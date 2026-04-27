package com.paul.simplesprint.features.monitoring.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.paul.simplesprint.features.race_session.SessionAnchorState
import com.paul.simplesprint.features.race_session.SessionCameraFacing
import com.paul.simplesprint.features.race_session.SessionDeviceRole
import com.paul.simplesprint.features.race_session.SessionOperatingMode
import com.paul.simplesprint.features.race_session.sessionCameraFacingLabel
import com.paul.simplesprint.features.race_session.sessionDeviceRoleLabel
import com.paul.simplesprint.sensor_native.SensorNativePreviewViewFactory
import com.paul.simplesprint.ui.components.PrimaryButton
import com.paul.simplesprint.ui.components.SectionHeader
import com.paul.simplesprint.ui.components.SprintSyncCard
import kotlin.math.roundToInt

data class ConnectedDeviceMonitoringCardUiState(
    val stableDeviceId: String?,
    val endpointId: String,
    val deviceName: String,
    val role: SessionDeviceRole,
    val latencyMs: Int?,
    val clockSynced: Boolean,
    val analysisWidth: Int?,
    val analysisHeight: Int?,
    val sensitivity: Int,
    val connected: Boolean,
)

@Composable
internal fun MonitoringSummaryCard(
    isHost: Boolean,
    controllerOnlyHost: Boolean,
    localRole: SessionDeviceRole,
    localCameraFacing: SessionCameraFacing,
    showDebugInfo: Boolean,
    connectionTypeLabel: String,
    syncModeLabel: String,
    latencyMs: Int?,
    localAnalysisResolutionLabel: String,
    userMonitoringEnabled: Boolean,
    onSetMonitoringEnabled: (Boolean) -> Unit,
    onAssignLocalCameraFacing: (SessionCameraFacing) -> Unit,
    effectiveShowPreview: Boolean,
    onShowPreviewChanged: (Boolean) -> Unit,
    sensitivity: Float,
    onUpdateSensitivity: (Float) -> Unit,
    previewViewFactory: SensorNativePreviewViewFactory,
    roiCenterX: Double,
    roiWidth: Double,
    operatingMode: SessionOperatingMode,
    discoveredDisplayHosts: Map<String, String>,
    displayConnectedHostName: String?,
    displayDiscoveryActive: Boolean,
    anchorDeviceName: String?,
    anchorState: SessionAnchorState,
    clockLockReasonLabel: String,
    onStartDisplayDiscovery: () -> Unit,
    onConnectDisplayHost: (String) -> Unit,
    onResetRun: () -> Unit,
) {
    val latencyLabel = when (syncModeLabel) {
        "NTP" -> if (latencyMs == null) "-" else "$latencyMs ms"
        "GPS" -> "GPS"
        else -> "-"
    }

    SprintSyncCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (controllerOnlyHost && operatingMode != SessionOperatingMode.SINGLE_DEVICE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Controller mode active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "This host handles connections and race orchestration only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    if (shouldShowMonitoringConnectionDebugInfo(showDebugInfo)) {
                        Text(
                            "Connection: $connectionTypeLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Sync: $syncModeLabel · Latency: $latencyLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Detection: $localAnalysisResolutionLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Anchor: ${anchorDeviceName ?: "-"} · State: ${anchorState.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Clock Lock: $clockLockReasonLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }
            } else if (operatingMode == SessionOperatingMode.SINGLE_DEVICE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (shouldShowMonitoringConnectionDebugInfo(showDebugInfo)) {
                        Text(
                            "Connection: $connectionTypeLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Sync: $syncModeLabel · Latency: $latencyLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Detection: $localAnalysisResolutionLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Anchor: ${anchorDeviceName ?: "-"} · State: ${anchorState.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        Text(
                            "Clock Lock: $clockLockReasonLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                    if (shouldShowMonitoringSensitivityControl(localRole, showDebugInfo)) {
                        Text(
                            "Sensitivity ${String.format("%.0f", sensitivity)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Slider(
                            value = sensitivity,
                            onValueChange = onUpdateSensitivity,
                            valueRange = SENSITIVITY_MIN.toFloat()..SENSITIVITY_MAX.toFloat(),
                            steps = SENSITIVITY_MAX - SENSITIVITY_MIN - 1,
                        )
                    }
                    if (shouldShowSingleDeviceCameraFacingToggle(operatingMode)) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    onClick = { onAssignLocalCameraFacing(SessionCameraFacing.REAR) },
                                    selected = localCameraFacing == SessionCameraFacing.REAR,
                                    label = { Text("Rear") },
                                )
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    onClick = { onAssignLocalCameraFacing(SessionCameraFacing.FRONT) },
                                    selected = localCameraFacing == SessionCameraFacing.FRONT,
                                    label = { Text("Front") },
                                )
                            }
                        }
                    }
                    if (shouldShowMonitoringPreview(operatingMode, effectiveShowPreview)) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            PreviewSurface(
                                previewViewFactory = previewViewFactory,
                                roiCenterX = roiCenterX,
                                roiWidth = roiWidth,
                            )
                        }
                    }
                    if (shouldShowDisplayRelayControls(operatingMode)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PrimaryButton(
                                text = if (displayDiscoveryActive) "Display: Discovering" else "Display",
                                onClick = onStartDisplayDiscovery,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = onResetRun,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Reset")
                            }
                        }
                        if (displayConnectedHostName != null) {
                            Text(
                                text = "Connected to $displayConnectedHostName",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        val hosts = discoveredDisplayHosts.entries.toList()
                        if (hosts.isNotEmpty()) {
                            hosts.forEach { host ->
                                OutlinedButton(
                                    onClick = { onConnectDisplayHost(host.key) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Join ${host.value}")
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonitoringPreviewInfoPanel(
                        isHost = isHost,
                        controllerOnlyHost = controllerOnlyHost,
                        localRole = localRole,
                        localCameraFacing = localCameraFacing,
                        showDebugInfo = showDebugInfo,
                        connectionTypeLabel = connectionTypeLabel,
                        syncModeLabel = syncModeLabel,
                        latencyLabel = latencyLabel,
                        localAnalysisResolutionLabel = localAnalysisResolutionLabel,
                        userMonitoringEnabled = userMonitoringEnabled,
                        onSetMonitoringEnabled = onSetMonitoringEnabled,
                        onAssignLocalCameraFacing = onAssignLocalCameraFacing,
                        effectiveShowPreview = effectiveShowPreview,
                        onShowPreviewChanged = onShowPreviewChanged,
                        sensitivity = sensitivity,
                        onUpdateSensitivity = onUpdateSensitivity,
                        operatingMode = operatingMode,
                        discoveredDisplayHosts = discoveredDisplayHosts,
                        displayConnectedHostName = displayConnectedHostName,
                        displayDiscoveryActive = displayDiscoveryActive,
                        onStartDisplayDiscovery = onStartDisplayDiscovery,
                        onConnectDisplayHost = onConnectDisplayHost,
                    )
                    if (shouldShowMonitoringPreview(operatingMode, effectiveShowPreview)) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            PreviewSurface(
                                previewViewFactory = previewViewFactory,
                                roiCenterX = roiCenterX,
                                roiWidth = roiWidth,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HostConnectedDeviceCards(
    cards: List<ConnectedDeviceMonitoringCardUiState>,
    showDebugInfo: Boolean,
    onRequestRemoteResync: (String) -> Unit,
    onUpdateRemoteSensitivity: (String, Int) -> Unit,
) {
    SprintSyncCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Connected Device Cards")
            cards.forEach { card ->
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(card.deviceName, fontWeight = FontWeight.SemiBold)
                        val roleLabel = sessionDeviceRoleLabel(card.role)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF3EFF7))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Role: $roleLabel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            OutlinedButton(
                                onClick = { onRequestRemoteResync(card.endpointId) },
                                enabled = card.connected,
                            ) {
                                Text("Resync")
                            }
                        }
                        Text(
                            "Latency: ${card.latencyMs?.let { "$it ms" } ?: "-"} · Sync: ${if (card.clockSynced) "✓" else "✗"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                        if (showDebugInfo) {
                            val resolutionLabel = if (card.analysisWidth != null && card.analysisHeight != null) {
                                "${card.analysisWidth}x${card.analysisHeight}"
                            } else {
                                "-"
                            }
                            Text(
                                "Detection: $resolutionLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    if (shouldShowMonitoringSensitivityControl(card.role, showDebugInfo)) {
                        Text("Sensitivity ${card.sensitivity}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = card.sensitivity.toFloat(),
                            onValueChange = { updated ->
                                card.stableDeviceId?.let { stableDeviceId ->
                                    onUpdateRemoteSensitivity(stableDeviceId, updated.roundToInt())
                                }
                            },
                            valueRange = SENSITIVITY_MIN.toFloat()..SENSITIVITY_MAX.toFloat(),
                            steps = SENSITIVITY_MAX - SENSITIVITY_MIN - 1,
                            enabled = card.connected && card.stableDeviceId != null,
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitoringPreviewInfoPanel(
    isHost: Boolean,
    controllerOnlyHost: Boolean,
    localRole: SessionDeviceRole,
    localCameraFacing: SessionCameraFacing,
    showDebugInfo: Boolean,
    connectionTypeLabel: String,
    syncModeLabel: String,
    latencyLabel: String,
    localAnalysisResolutionLabel: String,
    userMonitoringEnabled: Boolean,
    onSetMonitoringEnabled: (Boolean) -> Unit,
    onAssignLocalCameraFacing: (SessionCameraFacing) -> Unit,
    effectiveShowPreview: Boolean,
    onShowPreviewChanged: (Boolean) -> Unit,
    sensitivity: Float,
    onUpdateSensitivity: (Float) -> Unit,
    operatingMode: SessionOperatingMode,
    discoveredDisplayHosts: Map<String, String>,
    displayConnectedHostName: String?,
    displayDiscoveryActive: Boolean,
    onStartDisplayDiscovery: () -> Unit,
    onConnectDisplayHost: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (shouldShowMonitoringRoleAndToggles(operatingMode) && localRole != SessionDeviceRole.UNASSIGNED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Role: ${sessionDeviceRoleLabel(localRole)}",
                    fontWeight = FontWeight.Bold,
                )
                if (!isHost) {
                    Text("Waiting for host...", color = Color.Gray, fontStyle = FontStyle.Italic)
                }
            }
        }
        if (shouldShowMonitoringConnectionDebugInfo(showDebugInfo)) {
            Text("Connection: $connectionTypeLabel", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(
                "Sync: $syncModeLabel · Latency: $latencyLabel",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
            Text(
                "Detection: $localAnalysisResolutionLabel",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
        if (shouldShowMonitoringSensitivityControl(localRole, showDebugInfo)) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Sensitivity ${String.format("%.0f", sensitivity)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = sensitivity,
                onValueChange = onUpdateSensitivity,
                valueRange = SENSITIVITY_MIN.toFloat()..SENSITIVITY_MAX.toFloat(),
                steps = SENSITIVITY_MAX - SENSITIVITY_MIN - 1,
            )
        }
        if (shouldShowMonitoringPreviewToggle(localRole, operatingMode)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Preview", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = effectiveShowPreview,
                    enabled = true,
                    onCheckedChange = onShowPreviewChanged,
                )
            }
        }
        if (!controllerOnlyHost && shouldShowSingleDeviceCameraFacingToggle(operatingMode)) {
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    onClick = { onAssignLocalCameraFacing(SessionCameraFacing.REAR) },
                    selected = localCameraFacing == SessionCameraFacing.REAR,
                    label = { Text("Rear") },
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    onClick = { onAssignLocalCameraFacing(SessionCameraFacing.FRONT) },
                    selected = localCameraFacing == SessionCameraFacing.FRONT,
                    label = { Text("Front") },
                )
            }
        }
        if (shouldShowDisplayRelayControls(operatingMode)) {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = if (displayDiscoveryActive) "Display: Discovering" else "Display",
                onClick = onStartDisplayDiscovery,
                modifier = Modifier.fillMaxWidth(),
            )
            if (displayConnectedHostName != null) {
                Text(
                    text = "Connected to $displayConnectedHostName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            val hosts = discoveredDisplayHosts.entries.toList()
            if (hosts.isNotEmpty()) {
                hosts.forEach { host ->
                    OutlinedButton(
                        onClick = { onConnectDisplayHost(host.key) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Join ${host.value}")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ClockWarningCard(text: String) {
    SprintSyncCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text("!", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PreviewSurface(previewViewFactory: SensorNativePreviewViewFactory, roiCenterX: Double, roiWidth: Double) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp)
            .clip(MaterialTheme.shapes.medium),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            factory = { context ->
                previewViewFactory.createPreviewView(context)
            },
            onRelease = { view ->
                previewViewFactory.detachPreviewView(view)
            },
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val normalized = roiCenterX.coerceIn(0.0, 1.0).toFloat()
            val centerX = size.width * normalized
            val centerY = size.height * 0.5f
            val squareSize = (size.width * roiWidth.coerceIn(0.03, 0.40).toFloat())
                .coerceAtLeast(8.dp.toPx())
                .coerceAtMost(size.height)
            drawRect(
                color = Color(0xFF005A8D),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = centerX - (squareSize / 2f),
                    y = centerY - (squareSize / 2f),
                ),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

internal fun shouldShowDisplayRelayControls(mode: SessionOperatingMode): Boolean =
    mode == SessionOperatingMode.SINGLE_DEVICE

internal fun shouldShowMonitoringRoleAndToggles(mode: SessionOperatingMode): Boolean =
    mode != SessionOperatingMode.SINGLE_DEVICE

internal fun shouldShowSingleDeviceCameraFacingToggle(mode: SessionOperatingMode): Boolean =
    mode == SessionOperatingMode.SINGLE_DEVICE

internal fun shouldShowMonitoringConnectionDebugInfo(showDebugInfo: Boolean): Boolean = showDebugInfo

internal fun shouldShowMonitoringSensitivityControl(role: SessionDeviceRole, debugEnabled: Boolean): Boolean =
    role != SessionDeviceRole.CONTROLLER || debugEnabled

private const val SENSITIVITY_MIN = 1
private const val SENSITIVITY_MAX = 100
private const val THRESHOLD_MIN = 0.001
private const val THRESHOLD_MAX = 0.08

internal fun sensitivityToThreshold(sensitivity: Int): Double {
    val clamped = sensitivity.coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX)
    val normalized = (clamped - SENSITIVITY_MIN).toDouble() / (SENSITIVITY_MAX - SENSITIVITY_MIN).toDouble()
    return THRESHOLD_MAX - normalized * (THRESHOLD_MAX - THRESHOLD_MIN)
}

internal fun thresholdToSensitivity(threshold: Double): Float {
    val clamped = threshold.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX)
    val normalized = (clamped - THRESHOLD_MIN) / (THRESHOLD_MAX - THRESHOLD_MIN)
    return (SENSITIVITY_MAX - normalized * (SENSITIVITY_MAX - SENSITIVITY_MIN)).toFloat()
}

internal fun shouldShowMonitoringPreview(mode: SessionOperatingMode, effectiveShowPreview: Boolean): Boolean =
    effectiveShowPreview

internal fun shouldShowMonitoringPreviewToggle(role: SessionDeviceRole, mode: SessionOperatingMode): Boolean =
    role != SessionDeviceRole.CONTROLLER && mode != SessionOperatingMode.DISPLAY_HOST

internal fun shouldShowRunDetailMetrics(mode: SessionOperatingMode): Boolean =
    mode != SessionOperatingMode.SINGLE_DEVICE

internal fun shouldShowCameraFpsInfo(showDebugInfo: Boolean): Boolean = showDebugInfo
