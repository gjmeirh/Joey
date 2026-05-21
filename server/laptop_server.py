"""
Joey Gamepad Server
Receives WebSocket gamepad input from the Android app and drives a virtual
Xbox 360 controller via ViGEm / vgamepad.

Requirements:
  pip install vgamepad websockets

Run:
  python laptop_server.py [--port 8765]

The Android app connects to ws://<your-LAN-ip>:<port>
Find your LAN IP with: ipconfig  (Windows) / ip addr  (Linux)
"""

import asyncio
import json
import argparse
import socket
import sys

try:
    import vgamepad as vg
except ImportError:
    print("ERROR: vgamepad not installed.  Run:  pip install vgamepad")
    sys.exit(1)

try:
    import websockets
except ImportError:
    print("ERROR: websockets not installed.  Run:  pip install websockets")
    sys.exit(1)


# Xbox 360 button mapping  (Android btn-id → ViGEm flag)
BUTTON_MAP = {
    "a":          vg.XUSB_BUTTON.XUSB_GAMEPAD_A,
    "b":          vg.XUSB_BUTTON.XUSB_GAMEPAD_B,
    "x":          vg.XUSB_BUTTON.XUSB_GAMEPAD_X,
    "y":          vg.XUSB_BUTTON.XUSB_GAMEPAD_Y,
    "l1":         vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER,
    "r1":         vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER,
    "l3":         vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB,
    "r3":         vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB,
    "dpad_up":    vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP,
    "dpad_down":  vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN,
    "dpad_left":  vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    "dpad_right": vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
    "start":      vg.XUSB_BUTTON.XUSB_GAMEPAD_START,
    "select":     vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK,
    "home":       vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE,
}


class JoeyServer:
    def __init__(self):
        self.gamepad = vg.VX360Gamepad()
        self.clients: set = set()

    def apply_state(self, data: dict) -> None:
        if data.get("type") != "gamepad":
            return

        axes = data.get("axes", {})
        buttons = data.get("buttons", {})

        # Analog sticks  (Android Y axis: down = +1, Xbox expects up = +1 → invert Y)
        self.gamepad.left_joystick_float(
            x_value_float=float(axes.get("left_x",  0.0)),
            y_value_float=-float(axes.get("left_y", 0.0)),
        )
        self.gamepad.right_joystick_float(
            x_value_float=float(axes.get("right_x",  0.0)),
            y_value_float=-float(axes.get("right_y", 0.0)),
        )

        # Triggers  (0.0 → 1.0)
        self.gamepad.left_trigger_float(value_float=float(axes.get("left_trigger",  0.0)))
        self.gamepad.right_trigger_float(value_float=float(axes.get("right_trigger", 0.0)))

        # Digital buttons
        for btn_id, xbox_btn in BUTTON_MAP.items():
            if buttons.get(btn_id, False):
                self.gamepad.press_button(button=xbox_btn)
            else:
                self.gamepad.release_button(button=xbox_btn)

        self.gamepad.update()

    async def handler(self, websocket) -> None:
        addr = websocket.remote_address
        print(f"[+] Connected: {addr}")
        self.clients.add(websocket)
        try:
            async for raw in websocket:
                try:
                    data = json.loads(raw)
                    self.apply_state(data)
                except json.JSONDecodeError as e:
                    print(f"[!] Bad JSON from {addr}: {e}")
                except Exception as e:
                    print(f"[!] Error applying state from {addr}: {e}")
        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            self.clients.discard(websocket)
            # Release all inputs when client disconnects
            self.gamepad.reset()
            self.gamepad.update()
            print(f"[-] Disconnected: {addr}")

    async def serve(self, host: str, port: int) -> None:
        local_ips = get_local_ips()
        print("=" * 50)
        print("  Joey Gamepad Server")
        print("=" * 50)
        print(f"  Listening on port {port}")
        print()
        print("  On your Android phone, tap Menu → Connect and enter one of:")
        for ip in local_ips:
            print(f"    {ip}")
        print()
        print("  Press Ctrl+C to stop.")
        print("=" * 50)

        async with websockets.serve(self.handler, host, port):
            await asyncio.Future()  # run forever


def get_local_ips() -> list[str]:
    ips = []
    try:
        hostname = socket.gethostname()
        for info in socket.getaddrinfo(hostname, None):
            ip = info[4][0]
            if not ip.startswith("127.") and ":" not in ip:
                ips.append(ip)
    except Exception:
        pass
    if not ips:
        ips.append("(run  ipconfig  to find your LAN IP)")
    return list(dict.fromkeys(ips))  # deduplicate


def main() -> None:
    parser = argparse.ArgumentParser(description="Joey Gamepad WebSocket server")
    parser.add_argument("--host", default="0.0.0.0", help="Bind address (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8765, help="Port (default: 8765)")
    args = parser.parse_args()

    server = JoeyServer()
    try:
        asyncio.run(server.serve(args.host, args.port))
    except KeyboardInterrupt:
        print("\nServer stopped.")


if __name__ == "__main__":
    main()
