# Release Icon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use the provided high-fidelity PNG as the release icon for the Android app, supporting both modern adaptive icons and legacy density-specific icons.

**Architecture:**
- Create a `release` source set in `app/src/release/res` to override `main` icons for release builds.
- Generate adaptive icon foreground (432x432) with a 66dp safe zone (264x264 centered).
- Generate legacy icons (mdpi to xxxhdpi) including a rounded version.
- Store original source in `app/src/main/assets/`.

**Tech Stack:**
- Android Resource System
- ImageMagick (`magick`) for resizing and masking.

---

### Task 1: Create Directory Structure

**Files:**
- Create: `app/src/release/res/drawable`
- Create: `app/src/release/res/mipmap-mdpi`
- Create: `app/src/release/res/mipmap-hdpi`
- Create: `app/src/release/res/mipmap-xhdpi`
- Create: `app/src/release/res/mipmap-xxhdpi`
- Create: `app/src/release/res/mipmap-xxxhdpi`
- Create: `app/src/main/assets`

- [ ] **Step 1: Create directories**

Run: `New-Item -ItemType Directory -Path "app/src/release/res/drawable", "app/src/release/res/mipmap-mdpi", "app/src/release/res/mipmap-hdpi", "app/src/release/res/mipmap-xhdpi", "app/src/release/res/mipmap-xxhdpi", "app/src/release/res/mipmap-xxxhdpi", "app/src/main/assets" -Force`
Expected: Directories created.

- [ ] **Step 2: Commit directory structure**

Run: `git add app/src/release/res/ app/src/main/assets/ && git commit -m "chore: create release resource and assets directories"`
Expected: Commit successful.

---

### Task 2: Generate Adaptive Icon Foreground

**Files:**
- Create: `app/src/release/res/drawable/ic_launcher_foreground.png`

- [ ] **Step 1: Resize and center the source image for the adaptive foreground (432x432)**

We'll scale the 1024x1024 source to 264x264 (the 66dp safe zone) and center it on a 432x432 transparent canvas.

Run: `magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 264x264 -background none -gravity center -extent 432x432 "app/src/release/res/drawable/ic_launcher_foreground.png"`
Expected: File created at `app/src/release/res/drawable/ic_launcher_foreground.png`.

- [ ] **Step 2: Commit adaptive foreground**

Run: `git add app/src/release/res/drawable/ic_launcher_foreground.png && git commit -m "feat: add release adaptive icon foreground"`
Expected: Commit successful.

---

### Task 3: Generate Legacy Icons (Square)

**Files:**
- Create: `app/src/release/res/mipmap-mdpi/ic_launcher.png` (48x48)
- Create: `app/src/release/res/mipmap-hdpi/ic_launcher.png` (72x72)
- Create: `app/src/release/res/mipmap-xhdpi/ic_launcher.png` (96x96)
- Create: `app/src/release/res/mipmap-xxhdpi/ic_launcher.png` (144x144)
- Create: `app/src/release/res/mipmap-xxxhdpi/ic_launcher.png` (192x192)

- [ ] **Step 1: Generate square icons for each density**

Run:
```powershell
magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 48x48 "app/src/release/res/mipmap-mdpi/ic_launcher.png"
magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 72x72 "app/src/release/res/mipmap-hdpi/ic_launcher.png"
magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 96x96 "app/src/release/res/mipmap-xhdpi/ic_launcher.png"
magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 144x144 "app/src/release/res/mipmap-xxhdpi/ic_launcher.png"
magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 192x192 "app/src/release/res/mipmap-xxxhdpi/ic_launcher.png"
```
Expected: Files created.

- [ ] **Step 2: Commit legacy square icons**

Run: `git add app/src/release/res/mipmap-* && git commit -m "feat: add release legacy square icons"`
Expected: Commit successful.

---

### Task 4: Generate Legacy Icons (Round)

**Files:**
- Create: `app/src/release/res/mipmap-mdpi/ic_launcher_round.png` (48x48)
- Create: `app/src/release/res/mipmap-hdpi/ic_launcher_round.png` (72x72)
- Create: `app/src/release/res/mipmap-xhdpi/ic_launcher_round.png` (96x96)
- Create: `app/src/release/res/mipmap-xxhdpi/ic_launcher_round.png` (144x144)
- Create: `app/src/release/res/mipmap-xxxhdpi/ic_launcher_round.png` (192x192)

- [ ] **Step 1: Generate round icons using a circular mask**

We'll use a circular mask to crop the source image.

Run:
```powershell
$source = "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png"
# Create a circular mask and apply it
magick $source -resize 48x48 ( +clone -threshold -1 -draw "circle 24,24 24,1" ) -alpha off -compose copy_opacity -composite "app/src/release/res/mipmap-mdpi/ic_launcher_round.png"
magick $source -resize 72x72 ( +clone -threshold -1 -draw "circle 36,36 36,1" ) -alpha off -compose copy_opacity -composite "app/src/release/res/mipmap-hdpi/ic_launcher_round.png"
magick $source -resize 96x96 ( +clone -threshold -1 -draw "circle 48,48 48,1" ) -alpha off -compose copy_opacity -composite "app/src/release/res/mipmap-xhdpi/ic_launcher_round.png"
magick $source -resize 144x144 ( +clone -threshold -1 -draw "circle 72,72 72,1" ) -alpha off -compose copy_opacity -composite "app/src/release/res/mipmap-xxhdpi/ic_launcher_round.png"
magick $source -resize 192x192 ( +clone -threshold -1 -draw "circle 96,96 96,1" ) -alpha off -compose copy_opacity -composite "app/src/release/res/mipmap-xxxhdpi/ic_launcher_round.png"
```
Expected: Files created.

- [ ] **Step 2: Commit legacy round icons**

Run: `git add app/src/release/res/mipmap-* && git commit -m "feat: add release legacy round icons"`
Expected: Commit successful.

---

### Task 5: Store Original and Web Icon in Assets

**Files:**
- Create: `app/src/main/assets/release-icon-source.png`
- Create: `app/src/main/assets/ic_launcher-web.png` (512x512)

- [ ] **Step 1: Copy original to assets**

Run: `Copy-Item "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" "app/src/main/assets/release-icon-source.png"`
Expected: File copied.

- [ ] **Step 2: Generate 512x512 Play Store icon**

Run: `magick "C:\Users\paul\Downloads\gpt-image-1.5-high-fidelity_a_Can_you_create_an_ic.png" -resize 512x512 "app/src/main/assets/ic_launcher-web.png"`
Expected: File created.

- [ ] **Step 3: Commit assets**

Run: `git add app/src/main/assets/ && git commit -m "feat: store release icon source and web version in assets"`
Expected: Commit successful.
