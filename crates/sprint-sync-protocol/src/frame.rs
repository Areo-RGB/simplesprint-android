pub const FRAME_KIND_MESSAGE: u8 = 1;
pub const FRAME_KIND_CLOCK_SYNC: u8 = 2;
pub const FRAME_KIND_TELEMETRY: u8 = 3;
pub const MAX_FRAME_BYTES: usize = 1_048_576;

pub fn wrap_frame(kind: u8, payload: &[u8]) -> Vec<u8> {
  let mut frame = Vec::with_capacity(5 + payload.len());
  frame.push(kind);
  frame.extend_from_slice(&(payload.len() as u32).to_be_bytes());
  frame.extend_from_slice(payload);
  frame
}

pub fn parse_frame_header(buffer: &[u8]) -> Option<(u8, usize)> {
  if buffer.len() < 5 {
    return None;
  }

  let kind = buffer[0];
  let payload_length = u32::from_be_bytes([buffer[1], buffer[2], buffer[3], buffer[4]]) as usize;
  Some((kind, payload_length))
}
