"""Quick connection test server - run this instead of laptop_server.py to diagnose"""
import asyncio
import websockets
import socket

def get_ips():
    ips = []
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None):
            ip = info[4][0]
            if not ip.startswith("127.") and ":" not in ip:
                ips.append(ip)
    except:
        pass
    return list(set(ips)) or ["(could not detect)"]

async def handler(websocket):
    addr = websocket.remote_address
    print(f"\n✅ Phone connected from {addr}!")
    print("   Connection is working. You can now run laptop_server.py")
    try:
        async for msg in websocket:
            print(f"   Received {len(msg)} bytes")
    except Exception as e:
        print(f"   Disconnected: {e}")

async def main():
    print("\n== Joey Connection Test ==")
    print(f"Listening on port 8765")
    print(f"Your IP addresses: {get_ips()}")
    print("Waiting for phone to connect...\n")
    async with websockets.serve(handler, "0.0.0.0", 8765):
        await asyncio.Future()

asyncio.run(main())
