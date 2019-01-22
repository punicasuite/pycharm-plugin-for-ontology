#!/usr/bin/env node

const program = require("commander");
const { Project } = require("../src/project");
const { DebugServer } = require("../src/debug/server");

const info = require("../package.json");
program.version(info.version, "-v, --version");

program
  .command("project")
  .option("-i, --init <dir>", "Initializing project into the specified directory")
  .action(async function(cmd) {
    if (cmd.init) await Project.init(cmd.init);
  })
  .option("-c, --compile <file-or-dir>", "Compiling smart contracts")
  .action(async function(cmd) {
    if (cmd.compile) await Project.compile(cmd.compile);
  });

program
  .command("debug")
  .option("-t, --ticket <ticket>", "Creating new debugging session with ticket")
  .action(async function(cmd) {
    const srv = new DebugServer(cmd.ticket);
    await srv.start();
  });

program.parse(process.argv);
