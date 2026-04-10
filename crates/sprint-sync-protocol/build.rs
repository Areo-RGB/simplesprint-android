use std::env;
use std::path::PathBuf;
use std::process::Command;

fn main() {
  println!("cargo:rerun-if-changed=fbs/SprintSyncTelemetry.fbs");

  let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").expect("missing CARGO_MANIFEST_DIR"));
  let schema_path = manifest_dir.join("fbs/SprintSyncTelemetry.fbs");
  let output_dir = manifest_dir.join("src/generated");
  let generated_file = output_dir.join("SprintSyncTelemetry_generated.rs");

  std::fs::create_dir_all(&output_dir).expect("failed to create generated output directory");

  let generation = Command::new("flatc")
    .arg("--rust")
    .arg("-o")
    .arg(&output_dir)
    .arg(&schema_path)
    .output();

  match generation {
    Ok(result) if result.status.success() => {}
    Ok(result) => {
      if generated_file.exists() {
        println!(
          "cargo:warning=flatc failed (status: {:?}); using existing generated file",
          result.status.code()
        );
      } else {
        panic!(
          "flatc failed and no generated fallback exists: {}",
          String::from_utf8_lossy(&result.stderr)
        );
      }
    }
    Err(error) => {
      if generated_file.exists() {
        println!("cargo:warning=flatc unavailable ({error}); using existing generated file");
      } else {
        panic!("flatc unavailable and no generated fallback exists: {error}");
      }
    }
  }
}
