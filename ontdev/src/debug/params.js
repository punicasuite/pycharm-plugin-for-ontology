module.exports.processParams = function processParams(parameters, data) {
  return parameters.map(parameter => {
    return processData(parameter.name, data);
  });
};

function processData(name, data) {
  const value = data[name];
  const type = data[`${name}-type`];

  if (type === "Integer") {
    return Number(value);
  } else if (type === "Boolean") {
    return value === "true";
  } else if (type === "String") {
    return value;
  } else if (type === "ByteArray") {
    return new Buffer(value, "hex");
  } else if (type === "Address") {
    const address = Address.fromBase58(value);
    return address.toArray();
  } else if (type === "Array") {
    return processArrayData(name, data);
  } else if (type === "Map") {
    return processMapData(name, data);
  }
}

function processArrayData(name, data) {
  const items = [];

  for (let i = 0; data[`${name}[${i}]-type`] !== undefined; i++) {
    const itemName = `${name}[${i}]`;
    const item = processData(itemName, data);
    items.push(item);
  }

  return items;
}

function processMapData(name, data) {
  const items = new Map();

  for (let i = 0; data[`${name}[${i}]-type`] !== undefined; i++) {
    const itemName = data[`${name}[${i}]-name`];
    const itemIndexName = `${name}[${i}]`;
    const item = processData(itemIndexName, data);
    items.set(itemName, item);
  }

  return items;
}
