const { DebugServer } = require("./debug/server");

(async () => {
  await DebugServer.start();
  // await DebugServer.stop();
})();
