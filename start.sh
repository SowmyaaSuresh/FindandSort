#!/usr/bin/env bash
set -Eeuo pipefail

cd /app
export DISPLAY=:0
PORT="${PORT:-10000}"

echo "[startup] Starting virtual display..."
Xvfb :0 -screen 0 1024x768x16 -ac -nolisten tcp -noreset \
  >/tmp/xvfb.log 2>&1 &
XVFB_PID=$!

DISPLAY_READY=0
for attempt in $(seq 1 40); do
  if xdpyinfo -display :0 >/dev/null 2>&1; then
    DISPLAY_READY=1
    break
  fi
  if ! kill -0 "$XVFB_PID" 2>/dev/null; then
    echo "[startup] Xvfb exited unexpectedly"
    cat /tmp/xvfb.log || true
    exit 1
  fi
  sleep 1
done
if [ "$DISPLAY_READY" -ne 1 ]; then
  echo "[startup] Xvfb did not become ready"
  cat /tmp/xvfb.log || true
  exit 1
fi
echo "[startup] Virtual display is ready."

echo "[startup] Launching Java Swing game..."
java -Djava.awt.headless=false \
  -Dsun.java2d.xrender=false \
  -Dsun.java2d.opengl=false \
  -cp /app/out findsort.FindAndSortApp \
  >/tmp/find-sort-java.log 2>&1 &
JAVA_PID=$!

# Give Swing a moment to construct its main window; fail visibly if Java exits.
sleep 3
if ! kill -0 "$JAVA_PID" 2>/dev/null; then
  echo "[startup] Java application exited during startup:"
  cat /tmp/find-sort-java.log || true
  exit 1
fi
echo "[startup] Java process is running."

echo "[startup] Starting VNC server..."
x11vnc -display :0 -forever -shared -rfbport 5900 \
  -localhost -nopw -xkb -noxrecord -noxfixes -noxdamage \
  -defer 10 -wait 10 -threads \
  >/tmp/x11vnc.log 2>&1 &
VNC_PID=$!

VNC_READY=0
for attempt in $(seq 1 40); do
  if (echo > /dev/tcp/127.0.0.1/5900) >/dev/null 2>&1; then
    VNC_READY=1
    break
  fi
  if ! kill -0 "$VNC_PID" 2>/dev/null; then
    echo "[startup] x11vnc exited unexpectedly:"
    cat /tmp/x11vnc.log || true
    exit 1
  fi
  sleep 1
done
if [ "$VNC_READY" -ne 1 ]; then
  echo "[startup] VNC server did not become ready"
  cat /tmp/x11vnc.log || true
  exit 1
fi
echo "[startup] VNC server is ready."

# Serve a clean viewer that reconnects automatically after a transient disconnect.
cat > /usr/share/novnc/index.html <<'HTML'
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Find &amp; Sort</title>
<style>
html,body{height:100%;width:100%;margin:0;overflow:hidden;background:#111}
#screen{height:100%;width:100%;display:flex;align-items:center;justify-content:center}
#message{position:fixed;inset:0;display:flex;align-items:center;justify-content:center;
background:#111;color:#fff;font:16px Arial,sans-serif;text-align:center;padding:24px;z-index:5}
</style>
</head>
<body>
<div id="message">Starting Find &amp; Sort…</div>
<div id="screen"></div>
<script type="module">
import RFB from "./core/rfb.js";
const msg=document.getElementById("message");
const screen=document.getElementById("screen");
let retry=null, tries=0, rfb=null;
function show(t){msg.textContent=t;msg.style.display="flex";}
function reconnect(){
  if(retry) return;
  tries++;
  show("Reconnecting to Find & Sort… ("+tries+")");
  retry=setTimeout(()=>{retry=null;connect();},2500);
}
function connect(){
  try{
    show("Connecting to Find & Sort…");
    const scheme=location.protocol==="https:"?"wss":"ws";
    rfb=new RFB(screen,scheme+"://"+location.host+"/websockify");
    rfb.scaleViewport=true;
    rfb.resizeSession=false;
    rfb.showDotCursor=false;
    rfb.qualityLevel=6;
    rfb.compressionLevel=2;
    rfb.addEventListener("connect",()=>{tries=0;msg.style.display="none";});
    rfb.addEventListener("disconnect",()=>{rfb=null;reconnect();});
    rfb.addEventListener("securityfailure",()=>{show("Connection failed. Retrying…");reconnect();});
  }catch(e){reconnect();}
}
connect();
</script>
</body>
</html>
HTML

echo "[startup] Starting web viewer on port ${PORT}..."
exec websockify --heartbeat=30 --web=/usr/share/novnc "$PORT" 127.0.0.1:5900
