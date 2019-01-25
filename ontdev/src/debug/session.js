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

    this.conn.once("init", this.init.bind(this));
    this.conn.once("start", this.start.bind(this));

    this.conn.on("set-breakpoints", this.setBreakpoints.bind(this));

    this.conn.on("continue", this.continue.bind(this));
    this.conn.on("next", this.next.bind(this));
    this.conn.on("stepIn", this.stepIn.bind(this));
    this.conn.on("stepOut", this.stepOut.bind(this));

    this.conn.on("variables", this.variables.bind(this));
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
  }

  async setBreakpoints({ id, points }) {
    Object.keys(points).forEach(path => this.debugger.clearBreakpoints(path));
    Object.keys(points).forEach(path => {
      const lines = points[path];
      lines.forEach(line => this.debugger.setBreakpoint(path, line + 1));
    });
    this.conn.emit("RESP", { id, error: 0 });
  }

  async start({ id, method, data }) {
    try {
      const { result } = await this.debugger.start(method, data);
      console.log(`Result: ${this.getVariableValue(result)}`);
    } catch (e) {
      console.log(e);
    }
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

  async variables({ id, rf }) {
    const variableReference = rf || "0";

    const resp = { id, error: 0 };
    try {
      if (variableReference === "storage") {
        const vsVariables = this.getStorageVariables();
        resp.variables = vsVariables;
      } else {
        const [head, ...tail] = variableReference.split(".");
        const variables = this.debugger.getVariables(Number(head));
        const vsVariables = this.getVariablesDeep([head], tail, variables);

        resp.variables = vsVariables;
      }
    } catch (e) {
      console.log(e);
      resp.error = 1;
    }

    this.conn.emit("RESP", resp);
  }

  getStorageVariables() {
    const store = this.debugger.getStateStore();
    const variables = store.data;
    return Array.from(variables.entries()).map(([name, value]) => {
      if (value instanceof VM.StorageItem) {
        return {
          name,
          value: this.getStorageValue(value),
          type: this.getStorageValue(value),
          variablesReference: 0,
          storageItem: value
        };
      } else if (VM.isDeployCode(value)) {
        return {
          name,
          value: "Deployed code",
          type: "Deployed code",
          variablesReference: 0
        };
      } else {
        return {
          name,
          value: "unknown",
          type: "Unknown",
          variablesReference: 0
        };
      }
    });
  }

  getVariablesDeep(parentHead, parentTail, children) {
    const [head, ...tail] = parentTail;

    if (head === undefined) {
      // we already parsed whole tree, so return parent

      if (Array.isArray(children)) {
        // in case of Array use index as variable name
        return children.map((value, index) => {
          return this.createVariable(String(index), value, parentHead.join("."));
        });
      } else if (children instanceof Map) {
        // in case of Map, return with proper variable name
        return Array.from(children.entries()).map(([key, value]) => {
          if (typeof key !== "string") {
            key = key.getByteArray().toString();
          }

          return this.createVariable(key, value, parentHead.join("."));
        });
      }
    }

    let item;

    if (Array.isArray(children)) {
      const index = Number(head);
      if (index < children.length) {
        item = children[index];
      }
    } else if (children instanceof Map) {
      for (let [key, value] of children) {
        if (typeof key !== "string") {
          key = key.getByteArray().toString();
        }

        if (key === head) {
          item = value;
          break;
        }
      }
    }

    if (item === undefined || !(VM.isMapType(item) || VM.isArrayType(item))) {
      // item was not found or is of wrong type
      return [];
    }

    return this.getVariablesDeep([...parentHead, head], tail, item.value);
  }

  createVariable(name, item, refPrefix) {
    let variablesReference = 0;

    if (item !== undefined && (VM.isMapType(item) || VM.isArrayType(item))) {
      variablesReference = `${refPrefix}.${name}`;
    }

    return {
      name,
      type: this.getVariableType(item),
      value: this.getVariableValue(item),
      variablesReference,
      stackItem: item
    };
  }

  getVariableType(variable) {
    if (variable === undefined) {
      return "undefined";
    }

    if (VM.isArrayType(variable)) {
      return "array";
    } else if (VM.isBooleanType(variable)) {
      return "bool";
    } else if (VM.isIntegerType(variable)) {
      return "integer";
    } else if (VM.isByteArrayType(variable)) {
      return "string";
    } else if (VM.isMapType(variable)) {
      return "map";
    } else if (VM.isStructType(variable)) {
      return "struct";
    } else {
      return "unknown";
    }
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
    bp.line -= 1;
    this.conn.emit("BP", bp);
  }

  onStep(data) {
    this.conn.emit("STEP", data);
  }
};
