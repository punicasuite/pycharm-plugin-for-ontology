// copyright https://github.com/OntologyCommunityDevelopers/vscode-ext-ontology

const fs = require("fs");
const path = require("path");
const { compile, reverseBuffer } = require("ontology-ts-test");
const { Address } = require("ontology-ts-crypto");
const { ensureDirExist } = require("./util/fs");

module.exports.Compiler = class Compiler {
  static constructAbiName(contractPath) {
    const splitPath = path.parse(contractPath);
    const savePath = path.join(splitPath.dir, "build", splitPath.base);

    if (savePath.endsWith(".py")) {
      return savePath.replace(".py", "_abi.json");
    } else {
      return savePath.replace(".cs", "_abi.json");
    }
  }

  static constructAvmName(contractPath) {
    const splitPath = path.parse(contractPath);
    const savePath = path.join(splitPath.dir, "build", splitPath.base);

    if (savePath.endsWith(".py")) {
      return savePath.replace(".py", ".avm");
    } else {
      return savePath.replace(".cs", ".avm");
    }
  }

  static constructFuncMapName(contractPath) {
    const splitPath = path.parse(contractPath);
    const savePath = path.join(splitPath.dir, "build", splitPath.base);

    if (savePath.endsWith(".py")) {
      return savePath.replace(".py", "_funcMap.json");
    } else {
      return savePath.replace(".cs", "__funcMap.json");
    }
  }

  static constructDebugName(contractPath) {
    const splitPath = path.parse(contractPath);
    const savePath = path.join(splitPath.dir, "build", splitPath.base);

    if (savePath.endsWith(".py")) {
      return savePath.replace(".py", "_debug.json");
    } else {
      return savePath.replace(".cs", "__debug.json");
    }
  }

  async compileContract(contractPath) {
    console.log("Compiling " + contractPath);
    const result = await this.compileContractInPlace(contractPath);

    const abiPath = Compiler.constructAbiName(contractPath);
    const avmPath = Compiler.constructAvmName(contractPath);

    ensureDirExist(path.parse(avmPath).dir);

    await fs.promises.writeFile(avmPath, result.avm.toString("hex"));
    await fs.promises.writeFile(abiPath, result.abi);

    if (result.debug !== undefined) {
      const debugPath = Compiler.constructDebugName(contractPath);
      await fs.promises.writeFile(debugPath, JSON.stringify(result.debug));
    }

    if (result.funcMap !== undefined) {
      const funcMapPath = Compiler.constructFuncMapName(contractPath);
      await fs.promises.writeFile(funcMapPath, JSON.stringify(result.funcMap));
    }

    return result;
  }

  async compileContractIncremental(contractPath) {
    const abiPath = Compiler.constructAbiName(contractPath);
    const avmPath = Compiler.constructAvmName(contractPath);
    const debugPath = Compiler.constructDebugName(contractPath);
    const funcMapPath = Compiler.constructFuncMapName(contractPath);

    ensureDirExist(path.parse(avmPath).dir);

    try {
      const sourceStats = await fs.promises.stat(contractPath);
      const abiStats = await fs.promises.stat(abiPath);
      const avmStats = await fs.promises.stat(avmPath);
      const debugStats = await fs.promises.stat(debugPath);
      const funcMapStats = await fs.promises.stat(funcMapPath);

      if (
        sourceStats.mtimeMs > abiStats.mtimeMs ||
        sourceStats.mtimeMs > avmStats.mtimeMs ||
        sourceStats.mtimeMs > debugStats.mtimeMs ||
        sourceStats.mtimeMs > funcMapStats.mtimeMs
      ) {
        // if outdated
        return this.compileContract(contractPath);
      }
    } catch (e) {
      // fallback to compile
      return this.compileContract(contractPath);
    }

    const avm = new Buffer(await fs.promises.readFile(avmPath).toString(), "hex");

    return {
      abi: await fs.promises.readFile(abiPath),
      avm,
      debug: JSON.parse(await fs.promises.readFile(debugPath).toString()),
      funcMap: JSON.parse(await fs.promises.readFile(funcMapPath).toString()),
      contractHash: reverseBuffer(Address.fromVmCode(avm).toArray()).toString("hex")
    };
  }

  async compileContractInPlace(contractPath) {
    const code = await fs.promises.readFile(contractPath);

    let type;
    let url;

    if (contractPath.endsWith(".py")) {
      type = "Python";
      url = "https://smartxcompiler.ont.io/api/beta/python/compile";
    } else if (contractPath.endsWith(".cs")) {
      type = "CSharp";
      url = "https://smartxcompiler.ont.io/api/v1.0/csharp/compile";
    } else {
      throw new Error("Compile Error: Contract type is unknown.");
    }

    // disable SSL verify because of misconfigured compiler server
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
    const result = await compile({ code, type, url });
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = "1";

    return {
      avm: result.avm,
      abi: result.abi,
      contractHash: result.hash,
      debug: result.debug,
      funcMap: result.funcMap
    };
  }

  async compileInternal(fileOrDir) {
    const stats = await fs.promises.stat(fileOrDir);

    if (stats.isDirectory()) {
      const children = await fs.promises.readdir(fileOrDir);

      for (const child of children) {
        if (child.startsWith("__") || child.endsWith(".json") || child === "build") {
          continue;
        }

        const childPath = path.join(fileOrDir, child);
        await this.compileInternal(childPath);
      }
    } else if (stats.isFile()) {
      if (fileOrDir.endsWith(".py") || fileOrDir.endsWith(".cs")) {
        await this.compileContract(fileOrDir);
      }
    } else {
      throw new Error("Compile Error: contract path is wrong");
    }
  }
};
