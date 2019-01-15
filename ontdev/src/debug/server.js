require("babel-polyfill");
const tempDir = require("temp-dir");
const path = require("path");
const fs = require("fs");
const getPort = require("get-port");
const IO = require("socket.io");
const { DebugSession } = require("./session");

module.exports.DebugServer = class DebugServer {
  constructor(ticket) {
    this.ticket = ticket;
    this.sessColl = new Map();
  }

  getLockFilePath() {
    return path.resolve(tempDir, `./ontdev-debug-${this.ticket}.lock`);
  }

  async writeLock() {
    const lock = this.getLockFilePath();
    await fs.promises.writeFile(lock, JSON.stringify({ port: this.port }));
  }

  async start() {
    this.port = await getPort();

    this.io = IO();
    this.io.on("connect", conn => {
      const sess = new DebugSession(conn);
      this.sessColl.set(conn.id, sess);
      conn.on("disconnect", () => {
        this.sessColl.delete(conn.id);
      });
    });

    this.io.listen(this.port);
    await this.writeLock();

    console.log("Server is running at: " + this.port);
  }
};
