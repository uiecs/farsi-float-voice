# Farsi Float Voice

Android MVP for Persian voice typing with a movable translucent floating bubble.

## Features

- Floating, draggable bubble above other apps
- One tap to start Persian (`fa-IR`) speech recognition
- Tap again to stop/cancel
- Inserts recognized text into the currently focused editable field
- Works across apps that expose an editable accessibility node
- Persian-first setup screen
- No app backend required for the core feature

## Permissions

1. Microphone
2. Display over other apps
3. Accessibility service — used to locate the focused text field and insert the recognized text

Speech recognition itself uses the Android speech-recognition provider configured on the device.

## Build

Open the project in Android Studio or run:

`gradle assembleDebug`

GitHub Actions builds a debug APK on pushes to `main`.
