use jni::objects::{JByteArray, JClass, JLongArray, JString};
use jni::sys::{jbyteArray, jint, jlong, jlongArray, jstring};
use jni::JNIEnv;
use serde_json::{json, Value};
use sprint_sync_protocol::{clock_sync, telemetry};

const TYPE_TAG_TRIGGER_REQUEST: jint = 1;
const TYPE_TAG_SESSION_TRIGGER: jint = 2;
const TYPE_TAG_TIMELINE_SNAPSHOT: jint = 3;
const TYPE_TAG_SESSION_SNAPSHOT: jint = 4;
const TYPE_TAG_TRIGGER_REFINEMENT: jint = 5;
const TYPE_TAG_DEVICE_CONFIG_UPDATE: jint = 6;
const TYPE_TAG_CLOCK_RESYNC_REQUEST: jint = 7;
const TYPE_TAG_DEVICE_IDENTITY: jint = 8;
const TYPE_TAG_DEVICE_TELEMETRY: jint = 9;
const TYPE_TAG_LAP_RESULT: jint = 10;

fn to_java_byte_array(env: &mut JNIEnv, bytes: &[u8]) -> jbyteArray {
  env
    .byte_array_from_slice(bytes)
    .map(|array| array.into_raw())
    .unwrap_or(std::ptr::null_mut())
}

fn to_java_long_array(env: &mut JNIEnv, values: &[jlong]) -> jlongArray {
  let Ok(array) = env.new_long_array(values.len() as i32) else {
    return std::ptr::null_mut();
  };
  if env.set_long_array_region(&array, 0, values).is_err() {
    return std::ptr::null_mut();
  }
  array.into_raw()
}

fn to_java_string(env: &mut JNIEnv, value: &str) -> jstring {
  env
    .new_string(value)
    .map(|string| string.into_raw())
    .unwrap_or(std::ptr::null_mut())
}

fn role_from_name(name: &str) -> Option<telemetry::WireRole> {
  match name.trim().to_lowercase().as_str() {
    "start" => Some(telemetry::WireRole::Start),
    "split" | "split1" => Some(telemetry::WireRole::Split1),
    "split2" => Some(telemetry::WireRole::Split2),
    "split3" => Some(telemetry::WireRole::Split3),
    "split4" => Some(telemetry::WireRole::Split4),
    "stop" => Some(telemetry::WireRole::Stop),
    "display" => Some(telemetry::WireRole::Display),
    "unassigned" => Some(telemetry::WireRole::Unassigned),
    _ => None,
  }
}

fn role_to_name(role: &telemetry::WireRole) -> &'static str {
  match role {
    telemetry::WireRole::Unassigned => "unassigned",
    telemetry::WireRole::Start => "start",
    telemetry::WireRole::Split => "split1",
    telemetry::WireRole::Split1 => "split1",
    telemetry::WireRole::Split2 => "split2",
    telemetry::WireRole::Split3 => "split3",
    telemetry::WireRole::Split4 => "split4",
    telemetry::WireRole::Stop => "stop",
    telemetry::WireRole::Display => "display",
  }
}

fn camera_facing_from_name(name: Option<&str>) -> telemetry::CameraFacing {
  match name.unwrap_or("rear").trim().to_lowercase().as_str() {
    "front" => telemetry::CameraFacing::Front,
    _ => telemetry::CameraFacing::Rear,
  }
}

fn camera_facing_to_name(camera_facing: &telemetry::CameraFacing) -> &'static str {
  match camera_facing {
    telemetry::CameraFacing::Rear => "rear",
    telemetry::CameraFacing::Front => "front",
  }
}

fn required_string(object: &serde_json::Map<String, Value>, key: &str) -> Option<String> {
  let value = object.get(key)?.as_str()?.trim();
  if value.is_empty() {
    None
  } else {
    Some(value.to_string())
  }
}

fn optional_string(object: &serde_json::Map<String, Value>, key: &str) -> Option<String> {
  let raw = object.get(key)?;
  if raw.is_null() {
    return None;
  }
  let value = raw.as_str()?.trim();
  if value.is_empty() {
    None
  } else {
    Some(value.to_string())
  }
}

fn required_i64(object: &serde_json::Map<String, Value>, key: &str) -> Option<i64> {
  object.get(key)?.as_i64()
}

fn optional_i64(object: &serde_json::Map<String, Value>, key: &str) -> Option<i64> {
  let raw = object.get(key)?;
  if raw.is_null() {
    None
  } else {
    raw.as_i64()
  }
}

fn required_i32(object: &serde_json::Map<String, Value>, key: &str) -> Option<i32> {
  i32::try_from(object.get(key)?.as_i64()?).ok()
}

fn optional_i32(object: &serde_json::Map<String, Value>, key: &str) -> Option<i32> {
  let raw = object.get(key)?;
  if raw.is_null() {
    return None;
  }
  i32::try_from(raw.as_i64()?).ok()
}

fn parse_split_mark(value: &Value) -> Option<telemetry::ProtocolSplitMark> {
  let object = value.as_object()?;
  let role = role_from_name(object.get("role")?.as_str()?)?;
  if !matches!(
    role,
    telemetry::WireRole::Split1
      | telemetry::WireRole::Split2
      | telemetry::WireRole::Split3
      | telemetry::WireRole::Split4
  ) {
    return None;
  }

  Some(telemetry::ProtocolSplitMark {
    role,
    host_sensor_nanos: required_i64(object, "hostSensorNanos")?,
  })
}

fn parse_trigger_request(value: &Value) -> Option<telemetry::TriggerRequestMessage> {
  let object = value.as_object()?;
  Some(telemetry::TriggerRequestMessage {
    role: role_from_name(object.get("role")?.as_str()?)?,
    trigger_sensor_nanos: Some(required_i64(object, "triggerSensorNanos")?),
    mapped_host_sensor_nanos: optional_i64(object, "mappedHostSensorNanos"),
    source_device_id: Some(required_string(object, "sourceDeviceId")?),
    source_elapsed_nanos: Some(required_i64(object, "sourceElapsedNanos")?),
    mapped_anchor_elapsed_nanos: optional_i64(object, "mappedAnchorElapsedNanos"),
  })
}

fn parse_session_trigger(value: &Value) -> Option<telemetry::SessionTriggerMessage> {
  let object = value.as_object()?;
  Some(telemetry::SessionTriggerMessage {
    trigger_type: required_string(object, "triggerType")?,
    split_index: optional_i32(object, "splitIndex"),
    trigger_sensor_nanos: Some(required_i64(object, "triggerSensorNanos")?),
  })
}

fn parse_timeline_snapshot(value: &Value) -> Option<telemetry::TimelineSnapshotPayload> {
  let object = value.as_object()?;
  let mut host_split_marks = Vec::new();

  if let Some(raw_marks) = object.get("hostSplitMarks") {
    let marks = raw_marks.as_array()?;
    for mark in marks {
      host_split_marks.push(parse_split_mark(mark)?);
    }
  }

  let host_split_sensor_nanos = host_split_marks.iter().map(|mark| mark.host_sensor_nanos).collect();

  Some(telemetry::TimelineSnapshotPayload {
    r#type: "timeline_snapshot".to_string(),
    host_start_sensor_nanos: optional_i64(object, "hostStartSensorNanos"),
    host_stop_sensor_nanos: optional_i64(object, "hostStopSensorNanos"),
    host_split_marks,
    host_split_sensor_nanos,
    sent_elapsed_nanos: required_i64(object, "sentElapsedNanos")?,
  })
}

fn parse_session_snapshot(value: &Value) -> Option<telemetry::ProtocolSnapshot> {
  let object = value.as_object()?;
  let stage = required_string(object, "stage")?;
  let monitoring_active = object.get("monitoringActive").and_then(Value::as_bool).unwrap_or(false);

  let mut devices = Vec::new();
  let raw_devices = object.get("devices")?.as_array()?;
  for raw_device in raw_devices {
    let device = raw_device.as_object()?;
    devices.push(telemetry::ProtocolSnapshotDevice {
      id: required_string(device, "id")?,
      name: required_string(device, "name")?,
      role: role_from_name(device.get("role")?.as_str()?)?,
      camera_facing: camera_facing_from_name(device.get("cameraFacing").and_then(Value::as_str)),
      is_local: device.get("isLocal").and_then(Value::as_bool).unwrap_or(false),
    });
  }

  if devices.is_empty() {
    return None;
  }

  let timeline = object.get("timeline").and_then(Value::as_object).cloned().unwrap_or_default();
  let mut host_split_marks = Vec::new();
  if let Some(raw_marks) = timeline.get("hostSplitMarks") {
    for mark in raw_marks.as_array()? {
      host_split_marks.push(parse_split_mark(mark)?);
    }
  }

  let host_split_sensor_nanos = host_split_marks.iter().map(|mark| mark.host_sensor_nanos).collect();

  Some(telemetry::ProtocolSnapshot {
    r#type: "snapshot".to_string(),
    stage,
    monitoring_active,
    devices,
    timeline: telemetry::ProtocolTimelineSnapshot {
      host_start_sensor_nanos: optional_i64(&timeline, "hostStartSensorNanos"),
      host_stop_sensor_nanos: optional_i64(&timeline, "hostStopSensorNanos"),
      host_split_marks,
      host_split_sensor_nanos,
    },
    run_id: optional_string(object, "runId"),
    host_sensor_minus_elapsed_nanos: optional_i64(object, "hostSensorMinusElapsedNanos").unwrap_or(-1),
    host_gps_utc_offset_nanos: optional_i64(object, "hostGpsUtcOffsetNanos"),
    host_gps_fix_age_nanos: optional_i64(object, "hostGpsFixAgeNanos"),
    self_device_id: optional_string(object, "selfDeviceId"),
    anchor_device_id: optional_string(object, "anchorDeviceId"),
    anchor_state: optional_string(object, "anchorState"),
  })
}

fn parse_trigger_refinement(value: &Value) -> Option<telemetry::TriggerRefinementMessage> {
  let object = value.as_object()?;
  Some(telemetry::TriggerRefinementMessage {
    run_id: required_string(object, "runId")?,
    role: role_from_name(object.get("role")?.as_str()?)?,
    provisional_host_sensor_nanos: required_i64(object, "provisionalHostSensorNanos")?,
    refined_host_sensor_nanos: required_i64(object, "refinedHostSensorNanos")?,
  })
}

fn parse_device_config_update(value: &Value) -> Option<(String, i32)> {
  let object = value.as_object()?;
  let target = required_string(object, "targetStableDeviceId")?;
  let sensitivity = required_i32(object, "sensitivity")?;
  if !(1..=100).contains(&sensitivity) {
    return None;
  }
  Some((target, sensitivity))
}

fn parse_clock_resync_request(value: &Value) -> Option<i32> {
  let object = value.as_object()?;
  let sample_count = required_i32(object, "sampleCount")?;
  if !(3..=24).contains(&sample_count) {
    return None;
  }
  Some(sample_count)
}

fn parse_device_identity(value: &Value) -> Option<telemetry::DeviceIdentityMessage> {
  let object = value.as_object()?;
  Some(telemetry::DeviceIdentityMessage {
    stable_device_id: required_string(object, "stableDeviceId")?,
    device_name: required_string(object, "deviceName")?,
  })
}

fn parse_device_telemetry(value: &Value) -> Option<telemetry::DeviceTelemetryMessage> {
  let object = value.as_object()?;
  let sensitivity = required_i32(object, "sensitivity")?;
  if !(1..=100).contains(&sensitivity) {
    return None;
  }

  let latency_ms = optional_i32(object, "latencyMs");
  if latency_ms.is_some_and(|value| value < 0) {
    return None;
  }

  let analysis_width = optional_i32(object, "analysisWidth");
  let analysis_height = optional_i32(object, "analysisHeight");
  if (analysis_width.is_some() && analysis_height.is_none()) || (analysis_width.is_none() && analysis_height.is_some()) {
    return None;
  }
  if analysis_width.is_some_and(|value| value <= 0) || analysis_height.is_some_and(|value| value <= 0) {
    return None;
  }

  let timestamp_millis = required_i64(object, "timestampMillis")?;
  if timestamp_millis <= 0 {
    return None;
  }

  Some(telemetry::DeviceTelemetryMessage {
    stable_device_id: required_string(object, "stableDeviceId")?,
    role: role_from_name(object.get("role")?.as_str()?)?,
    sensitivity,
    latency_ms,
    clock_synced: object.get("clockSynced").and_then(Value::as_bool).unwrap_or(false),
    analysis_width,
    analysis_height,
    timestamp_millis,
  })
}

fn parse_lap_result(value: &Value) -> Option<telemetry::LapResultMessage> {
  let object = value.as_object()?;
  let started_sensor_nanos = required_i64(object, "startedSensorNanos")?;
  let stopped_sensor_nanos = required_i64(object, "stoppedSensorNanos")?;
  if stopped_sensor_nanos <= started_sensor_nanos {
    return None;
  }

  Some(telemetry::LapResultMessage {
    sender_device_name: required_string(object, "senderDeviceName")?,
    started_sensor_nanos,
    stopped_sensor_nanos,
  })
}

fn encode_telemetry_from_json(type_tag: jint, json_payload: &str) -> Option<Vec<u8>> {
  let decoded: Value = serde_json::from_str(json_payload).ok()?;
  match type_tag {
    TYPE_TAG_TRIGGER_REQUEST => telemetry::encode_trigger_request(&parse_trigger_request(&decoded)?),
    TYPE_TAG_SESSION_TRIGGER => {
      let message = parse_session_trigger(&decoded)?;
      telemetry::encode_session_trigger(
        &message.trigger_type,
        message.trigger_sensor_nanos?,
        message.split_index,
      )
    }
    TYPE_TAG_TIMELINE_SNAPSHOT => telemetry::encode_timeline_snapshot(&parse_timeline_snapshot(&decoded)?),
    TYPE_TAG_SESSION_SNAPSHOT => telemetry::encode_session_snapshot(&parse_session_snapshot(&decoded)?),
    TYPE_TAG_TRIGGER_REFINEMENT => {
      let message = parse_trigger_refinement(&decoded)?;
      telemetry::encode_trigger_refinement(
        &message.run_id,
        message.role,
        message.provisional_host_sensor_nanos,
        message.refined_host_sensor_nanos,
      )
    }
    TYPE_TAG_DEVICE_CONFIG_UPDATE => {
      let (target_stable_device_id, sensitivity) = parse_device_config_update(&decoded)?;
      telemetry::encode_device_config_update(&target_stable_device_id, sensitivity)
    }
    TYPE_TAG_CLOCK_RESYNC_REQUEST => telemetry::encode_clock_resync_request(parse_clock_resync_request(&decoded)?),
    TYPE_TAG_DEVICE_IDENTITY => telemetry::encode_device_identity(&parse_device_identity(&decoded)?),
    TYPE_TAG_DEVICE_TELEMETRY => telemetry::encode_device_telemetry(&parse_device_telemetry(&decoded)?),
    TYPE_TAG_LAP_RESULT => telemetry::encode_lap_result(&parse_lap_result(&decoded)?),
    _ => None,
  }
}

fn split_marks_to_json(split_marks: &[telemetry::ProtocolSplitMark]) -> Vec<Value> {
  split_marks
    .iter()
    .map(|split_mark| {
      json!({
        "role": role_to_name(&split_mark.role),
        "hostSensorNanos": split_mark.host_sensor_nanos,
      })
    })
    .collect()
}

fn decoded_telemetry_to_json(decoded: telemetry::DecodedTelemetryMessage) -> Value {
  match decoded {
    telemetry::DecodedTelemetryMessage::TriggerRequest(message) => json!({
      "type": "trigger_request",
      "role": role_to_name(&message.role),
      "triggerSensorNanos": message.trigger_sensor_nanos,
      "mappedHostSensorNanos": message.mapped_host_sensor_nanos,
      "sourceDeviceId": message.source_device_id,
      "sourceElapsedNanos": message.source_elapsed_nanos,
      "mappedAnchorElapsedNanos": message.mapped_anchor_elapsed_nanos,
    }),
    telemetry::DecodedTelemetryMessage::SessionTrigger(message) => json!({
      "type": "session_trigger",
      "triggerType": message.trigger_type,
      "splitIndex": message.split_index,
      "triggerSensorNanos": message.trigger_sensor_nanos,
    }),
    telemetry::DecodedTelemetryMessage::TimelineSnapshot(message) => {
      let split_marks = split_marks_to_json(&message.host_split_marks);
      let split_sensor_nanos: Vec<i64> = message.host_split_marks.iter().map(|mark| mark.host_sensor_nanos).collect();
      json!({
        "type": "timeline_snapshot",
        "hostStartSensorNanos": message.host_start_sensor_nanos,
        "hostStopSensorNanos": message.host_stop_sensor_nanos,
        "hostSplitMarks": split_marks,
        "hostSplitSensorNanos": split_sensor_nanos,
        "sentElapsedNanos": message.sent_elapsed_nanos,
      })
    }
    telemetry::DecodedTelemetryMessage::SessionSnapshot(message) => {
      let devices: Vec<Value> = message
        .devices
        .iter()
        .map(|device| {
          json!({
            "id": device.id,
            "name": device.name,
            "role": role_to_name(&device.role),
            "cameraFacing": camera_facing_to_name(&device.camera_facing),
            "isLocal": device.is_local,
          })
        })
        .collect();
      let split_marks = split_marks_to_json(&message.timeline.host_split_marks);
      let split_sensor_nanos: Vec<i64> = message
        .timeline
        .host_split_marks
        .iter()
        .map(|mark| mark.host_sensor_nanos)
        .collect();

      json!({
        "type": "snapshot",
        "stage": message.stage,
        "monitoringActive": message.monitoring_active,
        "devices": devices,
        "timeline": {
          "hostStartSensorNanos": message.timeline.host_start_sensor_nanos,
          "hostStopSensorNanos": message.timeline.host_stop_sensor_nanos,
          "hostSplitMarks": split_marks,
          "hostSplitSensorNanos": split_sensor_nanos,
        },
        "runId": message.run_id,
        "hostSensorMinusElapsedNanos": message.host_sensor_minus_elapsed_nanos,
        "hostGpsUtcOffsetNanos": message.host_gps_utc_offset_nanos,
        "hostGpsFixAgeNanos": message.host_gps_fix_age_nanos,
        "selfDeviceId": message.self_device_id,
        "anchorDeviceId": message.anchor_device_id,
        "anchorState": message.anchor_state,
      })
    }
    telemetry::DecodedTelemetryMessage::TriggerRefinement(message) => json!({
      "type": "trigger_refinement",
      "runId": message.run_id,
      "role": role_to_name(&message.role),
      "provisionalHostSensorNanos": message.provisional_host_sensor_nanos,
      "refinedHostSensorNanos": message.refined_host_sensor_nanos,
    }),
    telemetry::DecodedTelemetryMessage::DeviceConfigUpdate {
      target_stable_device_id,
      sensitivity,
    } => json!({
      "type": "device_config_update",
      "targetStableDeviceId": target_stable_device_id,
      "sensitivity": sensitivity,
    }),
    telemetry::DecodedTelemetryMessage::ClockResyncRequest { sample_count } => json!({
      "type": "clock_resync_request",
      "sampleCount": sample_count,
    }),
    telemetry::DecodedTelemetryMessage::DeviceIdentity(message) => json!({
      "type": "device_identity",
      "stableDeviceId": message.stable_device_id,
      "deviceName": message.device_name,
    }),
    telemetry::DecodedTelemetryMessage::DeviceTelemetry(message) => json!({
      "type": "device_telemetry",
      "stableDeviceId": message.stable_device_id,
      "role": role_to_name(&message.role),
      "sensitivity": message.sensitivity,
      "latencyMs": message.latency_ms,
      "clockSynced": message.clock_synced,
      "analysisWidth": message.analysis_width,
      "analysisHeight": message.analysis_height,
      "timestampMillis": message.timestamp_millis,
    }),
    telemetry::DecodedTelemetryMessage::LapResult(message) => json!({
      "type": "lap_result",
      "senderDeviceName": message.sender_device_name,
      "startedSensorNanos": message.started_sensor_nanos,
      "stoppedSensorNanos": message.stopped_sensor_nanos,
    }),
  }
}

#[no_mangle]
pub extern "system" fn Java_com_paul_sprintsync_protocol_NativeProtocol_encodeClockSyncRequestNative(
  mut env: JNIEnv,
  _class: JClass,
  client_send_elapsed_nanos: jlong,
) -> jbyteArray {
  let request = clock_sync::ClockSyncRequest {
    client_send_elapsed_nanos: client_send_elapsed_nanos as i64,
  };
  let encoded = clock_sync::encode_request(&request);
  to_java_byte_array(&mut env, &encoded)
}

#[no_mangle]
pub extern "system" fn Java_com_paul_sprintsync_protocol_NativeProtocol_decodeClockSyncRequestNative(
  mut env: JNIEnv,
  _class: JClass,
  payload: JByteArray,
) -> jlong {
  let Ok(bytes) = env.convert_byte_array(&payload) else {
    return -1;
  };
  let Some(decoded) = clock_sync::decode_request(&bytes) else {
    return -1;
  };
  decoded.client_send_elapsed_nanos as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_paul_sprintsync_protocol_NativeProtocol_encodeClockSyncResponseNative(
  mut env: JNIEnv,
  _class: JClass,
  client_send: jlong,
  host_receive: jlong,
  host_send: jlong,
) -> jbyteArray {
  let response = clock_sync::ClockSyncResponse {
    client_send_elapsed_nanos: client_send as i64,
    host_receive_elapsed_nanos: host_receive as i64,
    host_send_elapsed_nanos: host_send as i64,
  };
  let encoded = clock_sync::encode_response(&response);
  to_java_byte_array(&mut env, &encoded)
}

#[no_mangle]
pub extern "system" fn Java_com_paul_sprintsync_protocol_NativeProtocol_decodeClockSyncResponseNative(
  mut env: JNIEnv,
  _class: JClass,
  payload: JByteArray,
) -> jlongArray {
  let Ok(bytes) = env.convert_byte_array(&payload) else {
    return std::ptr::null_mut();
  };
  let Some(decoded) = clock_sync::decode_response(&bytes) else {
    return std::ptr::null_mut();
  };

  to_java_long_array(
    &mut env,
    &[
      decoded.client_send_elapsed_nanos as jlong,
      decoded.host_receive_elapsed_nanos as jlong,
      decoded.host_send_elapsed_nanos as jlong,
    ],
  )
}

#[no_mangle]
pub extern "system" fn Java_com_paul_sprintsync_protocol_NativeProtocol_encodeTelemetryEnvelopeNative(
  mut env: JNIEnv,
  _class: JClass,
  type_tag: jint,
  json_payload: JString,
) -> jbyteArray {
  let Ok(json_payload) = env.get_string(&json_payload) else {
    return std::ptr::null_mut();
  };

  let Some(encoded) = encode_telemetry_from_json(type_tag, &json_payload.to_string_lossy()) else {
    return std::ptr::null_mut();
  };

  to_java_byte_array(&mut env, &encoded)
}

#[no_mangle]
pub extern "system" fn Java_com_paul_sprintsync_protocol_NativeProtocol_decodeTelemetryEnvelopeNative(
  mut env: JNIEnv,
  _class: JClass,
  payload: JByteArray,
) -> jstring {
  let Ok(bytes) = env.convert_byte_array(&payload) else {
    return std::ptr::null_mut();
  };

  let Some(decoded) = telemetry::decode_telemetry_envelope(&bytes) else {
    return std::ptr::null_mut();
  };

  let json_value = decoded_telemetry_to_json(decoded);
  let Ok(json_payload) = serde_json::to_string(&json_value) else {
    return std::ptr::null_mut();
  };

  to_java_string(&mut env, &json_payload)
}
