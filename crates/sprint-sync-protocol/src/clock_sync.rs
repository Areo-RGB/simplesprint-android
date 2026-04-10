pub const VERSION: u8 = 1;
pub const TYPE_REQUEST: u8 = 1;
pub const TYPE_RESPONSE: u8 = 2;
pub const REQUEST_BYTES: usize = 10;
pub const RESPONSE_BYTES: usize = 26;

#[derive(Clone, Debug)]
pub struct ClockSyncRequest {
  pub client_send_elapsed_nanos: i64,
}

#[derive(Clone, Debug)]
pub struct ClockSyncResponse {
  pub client_send_elapsed_nanos: i64,
  pub host_receive_elapsed_nanos: i64,
  pub host_send_elapsed_nanos: i64,
}

pub fn encode_request(request: &ClockSyncRequest) -> [u8; REQUEST_BYTES] {
  let mut encoded = [0u8; REQUEST_BYTES];
  encoded[0] = VERSION;
  encoded[1] = TYPE_REQUEST;
  encoded[2..10].copy_from_slice(&request.client_send_elapsed_nanos.to_be_bytes());
  encoded
}

pub fn decode_request(payload: &[u8]) -> Option<ClockSyncRequest> {
  if payload.len() != REQUEST_BYTES {
    return None;
  }

  if payload[0] != VERSION || payload[1] != TYPE_REQUEST {
    return None;
  }

  let client_send_elapsed_nanos = i64::from_be_bytes(payload[2..10].try_into().ok()?);
  Some(ClockSyncRequest {
    client_send_elapsed_nanos,
  })
}

pub fn encode_response(response: &ClockSyncResponse) -> [u8; RESPONSE_BYTES] {
  let mut encoded = [0u8; RESPONSE_BYTES];
  encoded[0] = VERSION;
  encoded[1] = TYPE_RESPONSE;
  encoded[2..10].copy_from_slice(&response.client_send_elapsed_nanos.to_be_bytes());
  encoded[10..18].copy_from_slice(&response.host_receive_elapsed_nanos.to_be_bytes());
  encoded[18..26].copy_from_slice(&response.host_send_elapsed_nanos.to_be_bytes());
  encoded
}

pub fn decode_response(payload: &[u8]) -> Option<ClockSyncResponse> {
  if payload.len() != RESPONSE_BYTES {
    return None;
  }

  if payload[0] != VERSION || payload[1] != TYPE_RESPONSE {
    return None;
  }

  Some(ClockSyncResponse {
    client_send_elapsed_nanos: i64::from_be_bytes(payload[2..10].try_into().ok()?),
    host_receive_elapsed_nanos: i64::from_be_bytes(payload[10..18].try_into().ok()?),
    host_send_elapsed_nanos: i64::from_be_bytes(payload[18..26].try_into().ok()?),
  })
}
