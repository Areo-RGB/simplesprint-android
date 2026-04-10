use crate::state::{
  CameraFacing, DeviceIdentityMessage, DeviceTelemetryMessage, LapResultMessage, ProtocolSnapshot,
  ProtocolSnapshotDevice, ProtocolSplitMark, ProtocolTimelineSnapshot, SessionTriggerMessage,
  TimelineSnapshotPayload, TriggerRefinementMessage, TriggerRequestMessage, WireRole,
};
use sprint_sync_protocol::frame::{wrap_frame, FRAME_KIND_TELEMETRY};
use sprint_sync_protocol::telemetry as protocol_telemetry;

#[derive(Clone, Debug)]
pub enum DecodedTelemetryMessage {
  TriggerRequest(TriggerRequestMessage),
  SessionTrigger(SessionTriggerMessage),
  TimelineSnapshot(TimelineSnapshotPayload),
  SessionSnapshot(ProtocolSnapshot),
  TriggerRefinement(TriggerRefinementMessage),
  DeviceConfigUpdate { target_stable_device_id: String, sensitivity: i32 },
  ClockResyncRequest { sample_count: i32 },
  DeviceIdentity(DeviceIdentityMessage),
  DeviceTelemetry(DeviceTelemetryMessage),
  LapResult(LapResultMessage),
}

fn to_protocol_wire_role(role: &WireRole) -> protocol_telemetry::WireRole {
  match role {
    WireRole::Unassigned => protocol_telemetry::WireRole::Unassigned,
    WireRole::Start => protocol_telemetry::WireRole::Start,
    WireRole::Split => protocol_telemetry::WireRole::Split,
    WireRole::Split1 => protocol_telemetry::WireRole::Split1,
    WireRole::Split2 => protocol_telemetry::WireRole::Split2,
    WireRole::Split3 => protocol_telemetry::WireRole::Split3,
    WireRole::Split4 => protocol_telemetry::WireRole::Split4,
    WireRole::Stop => protocol_telemetry::WireRole::Stop,
    WireRole::Display => protocol_telemetry::WireRole::Display,
  }
}

fn to_protocol_camera_facing(camera_facing: &CameraFacing) -> protocol_telemetry::CameraFacing {
  match camera_facing {
    CameraFacing::Rear => protocol_telemetry::CameraFacing::Rear,
    CameraFacing::Front => protocol_telemetry::CameraFacing::Front,
  }
}

fn to_protocol_split_mark(split_mark: &ProtocolSplitMark) -> protocol_telemetry::ProtocolSplitMark {
  protocol_telemetry::ProtocolSplitMark {
    role: to_protocol_wire_role(&split_mark.role),
    host_sensor_nanos: split_mark.host_sensor_nanos,
  }
}

fn to_protocol_timeline_snapshot(snapshot: &ProtocolTimelineSnapshot) -> protocol_telemetry::ProtocolTimelineSnapshot {
  protocol_telemetry::ProtocolTimelineSnapshot {
    host_start_sensor_nanos: snapshot.host_start_sensor_nanos,
    host_stop_sensor_nanos: snapshot.host_stop_sensor_nanos,
    host_split_marks: snapshot.host_split_marks.iter().map(to_protocol_split_mark).collect(),
    host_split_sensor_nanos: snapshot.host_split_sensor_nanos.clone(),
  }
}

fn to_protocol_snapshot_device(device: &ProtocolSnapshotDevice) -> protocol_telemetry::ProtocolSnapshotDevice {
  protocol_telemetry::ProtocolSnapshotDevice {
    id: device.id.clone(),
    name: device.name.clone(),
    role: to_protocol_wire_role(&device.role),
    camera_facing: to_protocol_camera_facing(&device.camera_facing),
    is_local: device.is_local,
  }
}

fn to_protocol_snapshot(snapshot: &ProtocolSnapshot) -> protocol_telemetry::ProtocolSnapshot {
  protocol_telemetry::ProtocolSnapshot {
    r#type: snapshot.r#type.clone(),
    stage: snapshot.stage.clone(),
    monitoring_active: snapshot.monitoring_active,
    devices: snapshot
      .devices
      .iter()
      .map(to_protocol_snapshot_device)
      .collect(),
    timeline: to_protocol_timeline_snapshot(&snapshot.timeline),
    run_id: snapshot.run_id.clone(),
    host_sensor_minus_elapsed_nanos: snapshot.host_sensor_minus_elapsed_nanos,
    host_gps_utc_offset_nanos: snapshot.host_gps_utc_offset_nanos,
    host_gps_fix_age_nanos: snapshot.host_gps_fix_age_nanos,
    self_device_id: snapshot.self_device_id.clone(),
    anchor_device_id: snapshot.anchor_device_id.clone(),
    anchor_state: snapshot.anchor_state.clone(),
  }
}

fn to_protocol_timeline_payload(payload: &TimelineSnapshotPayload) -> protocol_telemetry::TimelineSnapshotPayload {
  protocol_telemetry::TimelineSnapshotPayload {
    r#type: payload.r#type.clone(),
    host_start_sensor_nanos: payload.host_start_sensor_nanos,
    host_stop_sensor_nanos: payload.host_stop_sensor_nanos,
    host_split_marks: payload.host_split_marks.iter().map(to_protocol_split_mark).collect(),
    host_split_sensor_nanos: payload.host_split_sensor_nanos.clone(),
    sent_elapsed_nanos: payload.sent_elapsed_nanos,
  }
}

pub fn decode_telemetry_envelope(payload: &[u8]) -> Option<DecodedTelemetryMessage> {
  let decoded = protocol_telemetry::decode_telemetry_envelope(payload)?;
  Some(match decoded {
    protocol_telemetry::DecodedTelemetryMessage::TriggerRequest(message) => {
      DecodedTelemetryMessage::TriggerRequest(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::SessionTrigger(message) => {
      DecodedTelemetryMessage::SessionTrigger(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::TimelineSnapshot(message) => {
      DecodedTelemetryMessage::TimelineSnapshot(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::SessionSnapshot(message) => {
      DecodedTelemetryMessage::SessionSnapshot(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::TriggerRefinement(message) => {
      DecodedTelemetryMessage::TriggerRefinement(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::DeviceConfigUpdate {
      target_stable_device_id,
      sensitivity,
    } => DecodedTelemetryMessage::DeviceConfigUpdate {
      target_stable_device_id,
      sensitivity,
    },
    protocol_telemetry::DecodedTelemetryMessage::ClockResyncRequest { sample_count } => {
      DecodedTelemetryMessage::ClockResyncRequest { sample_count }
    }
    protocol_telemetry::DecodedTelemetryMessage::DeviceIdentity(message) => {
      DecodedTelemetryMessage::DeviceIdentity(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::DeviceTelemetry(message) => {
      DecodedTelemetryMessage::DeviceTelemetry(message.into())
    }
    protocol_telemetry::DecodedTelemetryMessage::LapResult(message) => {
      DecodedTelemetryMessage::LapResult(message.into())
    }
  })
}

pub fn encode_session_snapshot(snapshot: &ProtocolSnapshot) -> Option<Vec<u8>> {
  let payload = protocol_telemetry::encode_session_snapshot(&to_protocol_snapshot(snapshot))?;
  Some(wrap_frame(FRAME_KIND_TELEMETRY, &payload))
}

pub fn encode_device_config_update(target_stable_device_id: &str, sensitivity: i32) -> Option<Vec<u8>> {
  let payload = protocol_telemetry::encode_device_config_update(target_stable_device_id, sensitivity)?;
  Some(wrap_frame(FRAME_KIND_TELEMETRY, &payload))
}

pub fn encode_clock_resync_request(sample_count: i32) -> Option<Vec<u8>> {
  let payload = protocol_telemetry::encode_clock_resync_request(sample_count)?;
  Some(wrap_frame(FRAME_KIND_TELEMETRY, &payload))
}

pub fn encode_trigger_refinement(
  run_id: &str,
  role: WireRole,
  provisional_host_sensor_nanos: i64,
  refined_host_sensor_nanos: i64,
) -> Option<Vec<u8>> {
  let payload = protocol_telemetry::encode_trigger_refinement(
    run_id,
    to_protocol_wire_role(&role),
    provisional_host_sensor_nanos,
    refined_host_sensor_nanos,
  )?;
  Some(wrap_frame(FRAME_KIND_TELEMETRY, &payload))
}

pub fn encode_session_trigger(trigger_type: &str, trigger_sensor_nanos: i64, split_index: Option<i32>) -> Option<Vec<u8>> {
  let payload = protocol_telemetry::encode_session_trigger(trigger_type, trigger_sensor_nanos, split_index)?;
  Some(wrap_frame(FRAME_KIND_TELEMETRY, &payload))
}

pub fn encode_timeline_snapshot(payload: &TimelineSnapshotPayload) -> Option<Vec<u8>> {
  let encoded_payload = protocol_telemetry::encode_timeline_snapshot(&to_protocol_timeline_payload(payload))?;
  Some(wrap_frame(FRAME_KIND_TELEMETRY, &encoded_payload))
}
