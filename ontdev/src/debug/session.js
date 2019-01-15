const { Debugger } = require("./debugger");
const fs = require("fs");
const read = fs.promises.readFile;
const VM = require("ontology-ts-vm");

module.exports.DebugSession = class DebugSession {
  constructor(conn) {
    this.conn = conn;

    this.debugger = new Debugger({
      onOutput: this.onOutput.bind(this),
      onBreakpoint: this.onBreakpoint.bind(this),
      onStep: this.onStep.bind(this)
    });

    // this.conn.once("init", this.init.bind(this));
    // this.conn.once("start", this.start.bind(this));
    this.conn.on("init", this.init.bind(this));
    this.conn.on("start", this.start.bind(this));

    this.conn.on("set-breakpoints", this.setBreakpoints.bind(this));

    this.conn.on("continue", this.continue.bind(this));
    this.conn.on("next", this.next.bind(this));
    this.conn.on("stepIn", this.stepIn.bind(this));
    this.conn.on("stepOut", this.stepOut.bind(this));
  }

  async init({ id, avm, abi, sourceFile, contractHash, debugInfo, funcMap }) {
    avm = await read(avm, { encoding: "utf8" });
    avm = new Buffer(avm, "hex");

    abi = await read(abi, { encoding: "utf8" });

    debugInfo = await read(debugInfo, { encoding: "utf8" });
    debugInfo = JSON.parse(debugInfo);

    funcMap = await read(funcMap, { encoding: "utf8" });
    funcMap = JSON.parse(funcMap);

    this.debugger.init({ avm, abi, sourceFile, contractHash, debugInfo, funcMap });
    this.conn.emit("RESP", { id, error: 0 });
    console.log("init");
  }

  async setBreakpoints({ id, points }) {
    Object.keys(points).forEach(path => this.debugger.clearBreakpoints(path));
    Object.keys(points).forEach(path => {
      const lines = points[path];
      lines.forEach(line => this.debugger.setBreakpoint(path, line));
    });
    this.conn.emit("RESP", { id, error: 0 });
    console.log("setBreakpoints", points);
  }

  async start({ id, method, data }) {
    try {
      const { result } = await this.debugger.start(method, data);
      console.log(`Result: ${this.getVariableValue(result)}`);
    } catch (e) {
      console.log("Error:" + e);
    }
    console.log("start");
    this.conn.emit("END", { id, error: 0 });
  }

  async continue({ id }) {
    this.debugger.continue();
    this.conn.emit("RESP", { id, error: 0 });
  }

  async next() {
    this.debugger.next();
  }

  async stepIn() {
    this.debugger.stepIn();
  }

  async stepOut() {
    this.debugger.stepOut();
  }

  getVariableValue(variable) {
    if (variable === undefined) {
      return "undefined";
    }

    if (VM.isArrayType(variable)) {
      return "Array";
    } else if (VM.isBooleanType(variable)) {
      return String(variable.value);
    } else if (VM.isIntegerType(variable)) {
      return variable.value.toString();
    } else if (VM.isByteArrayType(variable)) {
      return "0x" + variable.value.toString("hex");
    } else if (VM.isMapType(variable)) {
      return "Map";
    } else if (VM.isStructType(variable)) {
      return "Struct";
    } else {
      return "unknown";
    }
  }

  onOutput(data) {
    console.log(data);
  }

  onBreakpoint(bp) {
    this.conn.emit("BP", bp);
  }

  onStep(data) {
    this.conn.emit("STEP", data);
  }
};
