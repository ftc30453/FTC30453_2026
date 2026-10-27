#!/usr/bin/env bash
# Compiles your TeamCode against the simulator's stand-in FTC SDK and connects it to the
# Godot simulator. Re-run this after every code change (Ctrl+C to stop it).
#
#   ./run-bridge.sh                 # uses ../../TeamCode/src/main/java
#   TEAMCODE=/path/to/java ./run-bridge.sh
#   ./run-bridge.sh --port 7777
set -e
cd "$(dirname "$0")"

TEAMCODE="${TEAMCODE:-../../TeamCode/src/main/java}"

# Find a JDK: JAVA_HOME, then PATH, then the one bundled with Android Studio.
JAVAC=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  JAVAC="$JAVA_HOME/bin/javac"; JAVA="$JAVA_HOME/bin/java"
elif command -v javac >/dev/null 2>&1; then
  JAVAC="javac"; JAVA="java"
else
  for d in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
           "$HOME/android-studio/jbr" "/opt/android-studio/jbr" "/snap/android-studio/current/jbr"; do
    if [ -x "$d/bin/javac" ]; then JAVAC="$d/bin/javac"; JAVA="$d/bin/java"; break; fi
  done
fi
if [ -z "$JAVAC" ]; then
  echo "No Java compiler found. Install a JDK 17+ or set JAVA_HOME (Android Studio's 'jbr' folder works)."
  exit 1
fi

if [ ! -d "$TEAMCODE" ]; then
  echo "TeamCode folder not found at $TEAMCODE. Set TEAMCODE=/path/to/TeamCode/src/main/java"
  exit 1
fi

rm -rf build
mkdir -p build/classes
{ find src/main/java -name '*.java'; find "$TEAMCODE" -name '*.java'; } | sed 's/.*/"&"/' > build/sources.txt

echo "[bridge] Compiling TeamCode from $TEAMCODE ..."
"$JAVAC" --release 17 -nowarn -encoding UTF-8 -d build/classes @build/sources.txt
exec "$JAVA" -cp build/classes sim.SimMain "$@"
