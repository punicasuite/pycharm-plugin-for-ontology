// copyright https://github.com/OntologyCommunityDevelopers/vscode-ext-ontology

const fs = require("fs");
const path = require("path");

module.exports.ensureDirExist = p => {
  if (fs.existsSync(p)) {
    if (fs.statSync(p).isFile()) {
      throw new Error(`Path ${p} is a file. Should be a directory.`);
    }
  } else {
    fs.mkdirSync(p, { recursive: true });
  }
};

module.exports.fileNameFromPath = p => {
  const parsed = path.parse(p);
  return `${parsed.name}.${parsed.ext}`;
};
