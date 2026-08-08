#!/usr/bin/env python3
"""Smoke test TEH-Link NDJSON over USB CDC."""
import json
import sys
import time

import serial

PORT = sys.argv[1] if len(sys.argv) > 1 else "COM3"
BAUD = int(sys.argv[2]) if len(sys.argv) > 2 else 115200


def send(ser, cmd, id_=1):
    line = json.dumps({"cmd": cmd, "id": id_}, separators=(",", ":")) + "\n"
    ser.write(line.encode("utf-8"))
    ser.flush()
    print(">>", line.strip())
    deadline = time.time() + 3.0
    buf = b""
    while time.time() < deadline:
        chunk = ser.read(512)
        if not chunk:
            continue
        buf += chunk
        while b"\n" in buf:
            raw, buf = buf.split(b"\n", 1)
            text = raw.decode("utf-8", errors="replace").strip()
            if not text:
                continue
            print("<<", text[:800])
            try:
                return json.loads(text)
            except json.JSONDecodeError:
                continue
    print("!! timeout")
    return None


def main():
    print(f"Opening {PORT} @ {BAUD}")
    ser = serial.Serial(PORT, BAUD, timeout=0.4)
    time.sleep(0.35)
    ser.reset_input_buffer()

    ping = send(ser, "ping", 1)
    info = send(ser, "get_info", 2)
    ser.close()

    ok_ping = False
    if ping:
        data = ping.get("data") if isinstance(ping.get("data"), dict) else ping
        ok_ping = bool(isinstance(data, dict) and data.get("pong")) or ping.get("ok") is True
        proto = data.get("proto") if isinstance(data, dict) else None
        print("ping:", "ok" if ok_ping else "fail", "| proto=", proto)

    ok_info = False
    if info:
        data = info.get("data") if isinstance(info.get("data"), dict) else info
        if isinstance(data, dict):
            ok_info = bool(data.get("product"))
            print(
                "get_info:",
                "ok" if ok_info else "fail",
                f"| product={data.get('product')} version={data.get('version')} "
                f"codename={data.get('codename')}",
            )

    print("RESULT:", "PASS" if ok_ping and ok_info else "FAIL")
    return 0 if ok_ping and ok_info else 1


if __name__ == "__main__":
    raise SystemExit(main())
