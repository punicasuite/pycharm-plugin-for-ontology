const { initClient, isDeployed } = require("ontology-ts-test");

module.exports.Contract = class {
  static async isDeployed() {
    const client = initClient({ rpcAddress: rpc });
    const deployed = await isDeployed({ client, scriptHash: hash });

    return { deployed };
  }
};
