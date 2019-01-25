const {Box} = require("punica-ts/lib/box/box");
const git = require("isomorphic-git");
const fs = require("fs");
const util = require("util");
const tmp = require("tmp");
const rimraf = require("rimraf");
const {Compiler} = require("./compiler");
const copy = require("recursive-copy");

const tmpDir = util.promisify(tmp.dir);
const rma = util.promisify(rimraf);

git.plugins.set("fs", fs);

module.exports.Project = class {
  static async gitClone(repoUrl, repoToPath = '') {
    console.log('Downloading...');
    if (fs.existsSync(repoToPath)) {
      if (fs.statSync(repoToPath).isDirectory()) {
        if (fs.readdirSync(repoToPath).length > 0) {
          throw new Error("File already exists");
        }
      } else {
        throw new Error("File already exists");
      }
    }
    try {
      await git.clone({url: repoUrl, dir: repoToPath});
    } catch (e) {
      throw new Error("Network Error: " + e);
    }
  }

  static async init(dir) {
    try {
      await fs.promises.access(dir, fs.constants.R_OK | fs.constants.W_OK);
      const td = await tmpDir();
      console.log("Downloading into temporary directory: " + td);
      await this.gitClone("https://gitee.com/punica-box/init-box", td);

      console.log("Copying into project directory...");
      await copy(td, dir, {filter: ["**/*", "!.git"]});

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
