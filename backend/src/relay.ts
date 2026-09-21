import { Server } from "http";
import WebSocket, { WebSocketServer } from "ws";

const UPSTREAM_URL = "wss://8.229.22.124";
const RECONNECT_DELAY_MS = 2000;

export function attachPixelRelay(httpServer: Server, path = "/pixels"): WebSocketServer {
  // Shares the Express server's port; only handles upgrade requests for `path`
  const wss = new WebSocketServer({ server: httpServer, path });

  wss.on("connection", (client) => {
    console.log(`Client connected (${wss.clients.size} total)`);
    client.on("close", () => console.log(`Client disconnected (${wss.clients.size} total)`));
    client.on("error", (err) => console.error("Client error:", err.message));
  });

  function connectUpstream(): void {
    const upstream = new WebSocket(UPSTREAM_URL, {
      rejectUnauthorized: false, // only if you hit cert errors (raw IP)
    });

    upstream.on("open", () => console.log("Connected to upstream"));

    upstream.on("message", (data, isBinary) => {
      for (const client of wss.clients) {
        if (client.readyState === WebSocket.OPEN) {
          client.send(data, { binary: isBinary });
        }
      }
    });

    upstream.on("close", () => setTimeout(connectUpstream, RECONNECT_DELAY_MS));
    upstream.on("error", (err) => console.error("Upstream error:", err.message));
  }

  connectUpstream();
  return wss;
}