const { Box } = require("punica-ts/lib/box/box");
const git = require("isomorphic-git");
const fs = require("fs");
const util = require("util");
const tmp = require("tmp");
const rimraf = require("rimraf");
const Rsync = require("rsync");
const { Compiler } = require("./compiler");

const tmpDir = util.promisify(tmp.dir);
const rma = util.promisify(rimraf);

git.plugins.set("fs", fs);

module.exports.Project = class {
  static async init(dir) {
    try {
      await fs.promises.access(dir, fs.constants.R_OK | fs.constants.W_OK);
      const td = await tmpDir();
      console.log("Downloading into temporary directory: " + td);
      await new Box().init(td);

      let rsync = new Rsync()
        .source(td + "/")
        .exclude([".git"])
        .recursive()
        .destination(dir);
      rsync = util.promisify(rsync.execute.bind(rsync));

      console.log("Copying into project directory...");
      await rsync();

      console.log("Removing temporary directory...");
      await rma(td);

      console.log("Project initialized successfully.");
    } catch (e) {
      console.error("Error: " + e.message);
    }
  }

  static async compile(file) {
    try {
      await fs.promises.access(file, fs.constants.R_OK | fs.constants.W_OK);
      const c = new Compiler();
      await c.compileInternal(file);
      console.log("Compiled successfully.");
    } catch (e) {
      console.error("Error: " + e.message);
    }
  }
};
