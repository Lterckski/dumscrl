<p align="center">
  <img src="assets/logo.png" width="96" alt="dumscrl logo: a silver hand under an up arrow" />
</p>

<h1 align="center">dumscrl</h1>

<p align="center">Scroll TikTok, Instagram and Facebook hands-free with hand signs.</p>

---

dumscrl is an Android app for scrolling while your hands are busy (or greasy, because you're eating). The front camera watches your hand, MediaPipe hand tracking reads which sign you're making, and an Accessibility Service turns that sign into a swipe, a like, a photo flip or an app switch.

## Signs

| Sign | | Does |
| --- | --- | --- |
| Index finger up | ☝️ | Next video |
| Peace, thumb tucked | ✌️ | Previous video |
| Thumbs up | 👍 | Like |
| Thumb out to your left | 👈 | Next photo |
| Pinky out sideways | 🤙 | Previous photo |
| Any three fingers (thumb counts) | 🤟 | Next app |
| Any four fingers (thumb counts) | 🖖 | Previous app |
| Open palm, hold 2 s | ✋ | Pause or resume |

- Each sign has an on/off switch on the Signs tab. A switched-off sign does nothing, in either mode.
- A sign fires once you've held it still for about 1/3 second; a sign your hand only passes through while turning doesn't count. To fire the same sign again, lower your hand or change to another sign first.
- "Next app" and "Previous app" cycle through Facebook, Instagram and TikTok, whichever of them are installed.
- **The Camera tab is a practice area.** It shows what the camera sees, the tracked hand and the sign it reads ("Would do: Next video"), but it never acts on the phone.
- **Real control** starts only when you tap **Start** on Home and then switch to another app. While the Camera tab is open, signs don't act on anything, even with Start on.

## Status

Functionality first, and Android only.

- **Hand signs** is the default mode.
- **Flicks** is the other mode: see below.
- **Head** (nod and tilt) exists in the engine but is hidden in the app.

## Flicks mode

Pick **Flicks** in Settings. The modes don't overlap: you use one or the other. The thumb doesn't matter in any flick.

| Hand | Fingers | Flick | Does |
| --- | --- | --- | --- |
| Fingers up | Index | up or down | Next video |
| Fingers up | Index and middle | up or down | Previous video |
| Sideways | Index | sideways | Next photo |
| Sideways | Index and middle | sideways | Previous photo |

- The hand's direction decides the kind: with the fingers up only up/down movement counts, with the hand sideways only sideways movement. The fingers decide next or previous; the flick's direction doesn't matter.
- A flick is the fingertip moving at least 4 palm-lengths a second (and at least 0.6 of a palm), so it works at any distance from the camera.
- After each flick, every flick is ignored for a while: 0.6 s by default, adjustable from 0.3 to 2.0 s in Settings.
- Bringing your finger back doesn't count as another flick until the finger has rested for about 0.15 s.
- Signs that flicks already cover (index up, peace, thumb left, pinky) are off in this mode. Thumbs up, three, four and the palm-hold pause still work, with the fingers up only (a sideways hand is for photos).
- On the Camera tab, flicks show an arrow, "Would do: …", your fingertip speed against the threshold, and the wait left before the next flick counts.

## Stack

- Expo SDK 58, React Native 0.88.1, React 19.3, TypeScript 6.0
- Kotlin gesture engine as a local Expo module (`modules/dumscrl-engine`)
- `@react-native-community/slider` for the flick wait setting
- CameraX 1.7.0, MediaPipe Tasks Vision 0.10.26.1 (`hand_landmarker.task`, plus `face_landmarker.task` for head mode)
- JUnit 4 for the engine's unit tests

**How it fits together.** The React Native app is the UI: Home with the Start button and the setup checklist, then the Camera, Signs and Settings tabs. Everything else runs in Kotlin. `GestureService` is a foreground service that runs the front camera and hand tracking off the main thread. `HandPose` names the sign from the 21 hand landmarks, `HandDetector` decides when a held sign fires, and `FlickDetector` reads index-finger flicks. `GestureAccessibilityService` performs the resulting swipe, double tap or app switch. The JS side talks to the engine through the module's functions and events (`start`, `stop`, `setPractice`, `setFlickCooldown`, `onSign`, `onStatus`, `onFlickMeter`) and a native camera preview view.

## Install

1. On the phone, open the [latest release](https://github.com/Lterckski/dumscrl/releases/latest) and download `dumscrl.apk`.
2. Open it. Android asks to allow installs from your browser or Files app the first time; allow it, then install. Samsung phones with **Auto Blocker** on block apps from outside the Galaxy Store: turn it off under **Settings > Security and privacy > Auto Blocker**, install, and turn it back on if you like.
3. Follow [First run](#first-run) below.

Coming from an older build named `com.lterckski.gesturescroll`? It's a different app id, so uninstall the old one first; your settings start fresh.

## Requirements

- Node 24 and npm
- A JDK (built with Temurin 25)
- Android Studio, or the Android SDK with `adb` on your PATH
- A physical Android phone with USB debugging on. Samsung phones need **Auto Blocker** turned off to install over USB.

## Setup

```bash
npm install
npm run prebuild      # generates android/ and copies your debug keystore into it
```

Release builds are signed with your own key when `~/.gradle/gradle.properties` has `DUMSCRL_STORE_FILE`, `DUMSCRL_STORE_PASSWORD`, `DUMSCRL_KEY_ALIAS` and `DUMSCRL_KEY_PASSWORD` (see `plugins/withReleaseSigning.js`); otherwise they use the debug key. Keep the key out of the repo and back it up: an update has to be signed with the same key to install over an existing copy.

`prebuild --clean` regenerates `android/` from scratch, so add `android/local.properties` again afterwards (or set `ANDROID_HOME`):

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > android/local.properties
```

Build and install the release APK (arm64 only):

```bash
npm run build:apk
adb install -r android/app/build/outputs/apk/release/app-release.apk
```

Checks:

```bash
npm run typecheck     # TypeScript
npm run test:engine   # Kotlin engine unit tests
```

To watch the engine work: `adb logcat -s GestureScroll`.

## First run

1. Open dumscrl and allow the **camera**, plus **notifications** (the notification shows the last sign).
2. Under **Settings > Accessibility**, turn on **dumscrl**. A sideloaded app may show this switch greyed out: open dumscrl's App info, use the ⋮ menu to choose **Allow restricted settings**, then try again.
3. Practise on the **Camera** tab, then tap **Start** on Home and open TikTok, Instagram or Facebook.

## Privacy

- Camera frames are processed on the phone only. Nothing is stored or sent anywhere.
- The accessibility permission is used only to perform the gestures and to find photo carousels to scroll.

## Repo layout

```
App.tsx                  tabs and page transitions
src/                     UI: theme, shared components, sign list, engine hook
src/screens/             Home, Camera, Signs, Settings
modules/dumscrl-engine/  Kotlin engine (Expo module): camera service, hand and face tracking,
                         sign rules, accessibility actions, models in android/src/main/assets
assets/                  app icon and logo
plugins/                 Expo config plugin for release signing
android/                 generated by `npm run prebuild`, not in git
```

## Known limitations

- TikTok doesn't expose its photo carousel to accessibility actions, so photo flips are a drawn swipe across the photo.
- The Kotlin classes still live in `com.lterckski.gesturescroll`, from the app's earlier name; the app id is `com.lterckski.dumscrl`.
- Release builds shrink code with R8. MediaPipe, protobuf and Flogger have keep rules in `modules/dumscrl-engine/android/proguard-rules.pro`; a new native library may need one too.

## License

[MIT](LICENSE)

---

Built by [Lterckski](https://github.com/Lterckski) with help from Claude.
