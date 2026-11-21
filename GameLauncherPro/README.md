# Game Launcher Pro  ·  by TAHALABS

Personal-use Android game launcher for the Pixel 6a (Android 12+). Not for the Play Store.

## Open it
1. Unzip this folder.
2. Android Studio > File > Open > select the `GameLauncherPro` folder.
3. Wait for Gradle sync (needs internet the first time; it downloads Gradle 8.9 + libraries).
4. On the phone: Settings > About phone > tap Build number 7 times, then Developer options > USB debugging (or Wireless debugging).
5. Pick the Pixel 6a at the top of Android Studio and press Run.
   To get a shareable APK instead: Build > Build APK(s) -> app/build/outputs/apk/debug/app-debug.apk

## One-time permissions (Setup tab shows these live)
- Display over other apps  -> in-game HUD
- Do Not Disturb access    -> silence interruptions
- Notifications            -> session card with "End session"
- Advanced (optional, from a PC):
  adb shell pm grant com.tahalabs.gamelauncherpro android.permission.WRITE_SECURE_SETTINGS

## Layout
app/src/main/java/com/tahalabs/gamelauncherpro/
  MainActivity.kt        entry point
  engine/Booster.kt      RAM sweep, DND, battery saver, animations, restore logic
  engine/Perms.kt        permission status
  service/GameHudService.kt   session owner: HUD overlay, Live Guard, notification
  data/                  game detection, library, settings
  ui/                    Compose screens (Splash, Games, Boost, Setup, Session)
