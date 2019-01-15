#!/usr/bin/env node --no-warnings

const { DebugServer } = require("../src/debug/server");

require("yargs")
  .usage("$0 <cmd> [args]")
  .command(
    "debug [ticket]",
    "Debug with ticket",
    yargs => {
      yargs.positional("ticket", {
        type: "string",
        describe: "Debug with ticket"
      }).required("ticket", "Please specify ticket");
    },
    async argv => {
      const srv = new DebugServer(argv.ticket);
      await srv.start();
    }
  )
  .help().argv;
