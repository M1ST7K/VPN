#!/usr/bin/env bash
# Fresh real-renderer captures for typography pass 4 / Onest (debug harness, ru-RU, 420dpi).
set -u
export PATH=/home/ubuntu/android-sdk/platform-tools:$PATH
HERE="$(cd "$(dirname "$0")" && pwd)"
PROBE="$HERE/probe.py"
adb -s emulator-5554 shell settings put global animator_duration_scale 0
adb -s emulator-5554 shell settings put global transition_animation_scale 0
adb -s emulator-5554 shell settings put global window_animation_scale 0
adb -s emulator-5554 shell cmd locale set-app-locales com.hotfox.vpn --locales ru-RU

# viewport_dp -> px at 420dpi: 360x800=945x2100, 393x873=1032x2292, 412x915=1082x2402
run() { python3 "$PROBE" "$1" "$2" "$3" "$4" "$HERE/$5" || echo "FAILED $5"; }

S=1032x2292
if [ "${SKIP_ONBOARDING_ACTUAL:-0}" != 1 ]; then
run 01_SPLASH            $S 1.0  "HotFox"                   actual/01_splash.png
run 02_ONBOARD_CONNECT   $S 1.0  "У меня уже есть подписка" actual/02_connect.png
run 03_ONBOARD_AUTO      $S 1.0  "Использовать AUTO"        actual/03_auto.png
run 09_HTTPS_SUBSCRIPTION $S 1.0 "HTTPS-подписка"           actual/04_https.png
fi
run 05_DISCONNECTED      $S 1.0  "Не защищено"              actual/05_disconnected.png
run 06_CONNECTING_VISUAL $S 1.0  "Подключаем"               actual/06_connecting.png
run 07_PROTECTED_VISUAL  $S 1.0  "Вы защищены"              actual/07_connected.png

for vp in 945x2100:360 1082x2402:412; do
  px=${vp%%:*}; dp=${vp##*:}
  run 05_DISCONNECTED      $px 1.0 "Не защищено" matrix/05_home_$dp.png
  run 06_CONNECTING_VISUAL $px 1.0 "Подключаем"  matrix/06_connecting_$dp.png
  run 07_PROTECTED_VISUAL  $px 1.0 "Вы защищены" matrix/07_connected_$dp.png
done
for vp in 945x2100:360 1032x2292:393 1082x2402:412; do
  px=${vp%%:*}; dp=${vp##*:}
  run 05_DISCONNECTED      $px 1.15 "Не защищено" matrix/05_home_${dp}_fs115.png
  run 06_CONNECTING_VISUAL $px 1.15 "Подключаем"  matrix/06_connecting_${dp}_fs115.png
  run 07_PROTECTED_VISUAL  $px 1.15 "Вы защищены" matrix/07_connected_${dp}_fs115.png
done
run 09_HTTPS_SUBSCRIPTION 945x2100 1.0 "HTTPS-подписка"    matrix/04_https_360.png
run 03_ONBOARD_AUTO      945x2100  1.0 "Использовать AUTO" matrix/03_auto_360.png
run 02_ONBOARD_CONNECT   945x2100  1.0 "У меня уже есть подписка" matrix/02_connect_360.png
run 02_ONBOARD_CONNECT   $S 1.15 "У меня уже есть подписка" matrix/02_connect_fs115.png
run 03_ONBOARD_AUTO      $S 1.15 "Использовать AUTO"        matrix/03_auto_fs115.png
run 09_HTTPS_SUBSCRIPTION $S 1.15 "HTTPS-подписка"          matrix/04_https_fs115.png

adb -s emulator-5554 shell settings put system font_scale 1.0
adb -s emulator-5554 shell wm size $S
echo CAPTURE_DONE
