Ext.onReady(function() {
  function getScrollbarWidth() {
    var outer = document.createElement("div");
    outer.style.visibility = "hidden";
    outer.style.width = "100px";
    document.body.appendChild(outer);

    var widthNoScroll = outer.offsetWidth;
    outer.style.overflow = "scroll";

    var inner = document.createElement("div");
    inner.style.width = "100%";
    outer.appendChild(inner);

    var widthWithScroll = inner.offsetWidth;
    outer.parentNode.removeChild(outer);

    return widthNoScroll - widthWithScroll;
  }

  function showErr(msg) {
    Ext.MessageBox.show({
      title: "Error",
      msg: msg,
      buttons: Ext.MessageBox.OK,
      icon: Ext.MessageBox.ERROR
    });
  }

  window.showErr = showErr;

  var form = {
    wallet: null
  };

  function updateWallet(path) {
    if (!/\.json$/.test(path)) {
      showErr("Invalid wallet file.");
      return false;
    }

    var wallet = java.call("_settings_", "getWalletJson", [path]);
    if (wallet === "") {
      showErr("Unable to read wallet file.");
      return false;
    }
    try {
      wallet = JSON.parse(wallet);
    } catch (e) {
      showErr("Deformed wallet file.");
      return false;
    }
    if (!Array.isArray(wallet.accounts)) {
      showErr("Deformed wallet file.");
      return false;
    } else if (wallet.accounts.length === 0) {
      showErr("Empty wallet file.");
      return false;
    }

    form.wallet = wallet;
    updatePayers();
    return true;
  }

  var payerStore = new Ext.data.ArrayStore({
    id: 0,
    fields: ["val", "label"],
    data: []
  });

  var networkStore = new Ext.data.ArrayStore({
    id: 1,
    fields: ["val", "label"],
    data: []
  });

  var getField = function(name) {
    return formPanel.getForm().findField(name);
  };

  var loadNetwork = function() {
    var cfg = java.call("_settings_", "getNetworkConfigs");
    if (cfg === "") {
      showErr(
        "Unable to load network configs, please ensure the file punica-config.json is readable"
      );
      return;
    }
    try {
      cfg = JSON.parse(cfg);
    } catch (e) {
      showErr("Deformed network configs in punica-config.json");
      return;
    }
    var networks = cfg.networks;
    var records = [];
    var networkNames = Object.keys(networks);
    networkNames.forEach(function(nn) {
      records.push([nn, nn]);
    });
    networkStore.loadData(records, false);
    formPanel
      .getForm()
      .findField("network-default")
      .setValue(cfg.defaultNet);

    var pn = networks["privateNet"];
    formPanel
      .getForm()
      .findField("network-private")
      .setValue(pn.host + ":" + pn.port);
  };

  var loadConfig = function() {
    var cfg = java.call("_settings_", "getConfigJson");
    try {
      cfg = JSON.parse(cfg);
    } catch (e) {
      showErr("Unable to load configs, some errors occur in config json files");
      return;
    }

    var isWalletOk = updateWallet(cfg.wallet);
    if (!isWalletOk) return;

    getField("wallet-file").setValue(cfg.wallet);

    getField("deploy-gas-limit").setValue(cfg.deploy.gasLimit);
    getField("deploy-gas-price").setValue(cfg.deploy.gasPrice);
    getField("deploy-payer").setValue(cfg.deploy.payer);

    getField("invoke-gas-limit").setValue(cfg.invoke.gasLimit);
    getField("invoke-gas-price").setValue(cfg.invoke.gasPrice);
    getField("invoke-payer").setValue(cfg.invoke.payer);
  };

  function updatePayers() {
    var records = [];
    var accounts = form.wallet.accounts;
    accounts.forEach(function(acc) {
      records.push([acc.address, acc.address]);
    });
    payerStore.loadData(records, false);
    getField("deploy-payer").setValue(accounts[0].address);
    getField("invoke-payer").setValue(accounts[0].address);
  }

  var validateGas = function(v) {
    if (!/^0|[1-9][0-9]*$/.test(v)) return false;
    v = parseInt(v);
    return !isNaN(v) && isFinite(v);
  };

  var kUrlRegexp = /^(?:(ws|wss|http|https):)\/\/([a-zA-Z0-9.-]+)(?::(\d+))$/;

  var formPanel = new Ext.FormPanel({
    labelWidth: 75,
    bodyStyle: "padding:5px 5px 0",
    autoHeight: true,
    width: Ext.getBody().getViewSize().width - getScrollbarWidth(),

    items: [
      {
        xtype: "fieldset",
        title: "Wallet",
        labelAlign: "top",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        items: [
          {
            xtype: "compositefield",
            fieldLabel: "File",
            tips: "Wallet file used during deploy and invoke.",
            labelAlign: "top",
            items: [
              {
                xtype: "textfield",
                name: "wallet-file",
                width: 230,
                listeners: {
                  blur: function(c) {
                    form.wallet = null;
                    c.clearInvalid();
                    var path = c.getValue();
                    if (!updateWallet(path)) {
                      c.markInvalid("Invalid wallet file.");
                    }
                  }
                }
              },
              {
                xtype: "button",
                name: "btn-choose-wallet",
                text: "Choose",
                width: 30,
                onClick: function() {
                  var input = getField("wallet-file");
                  input.clearInvalid();

                  java.call("_settings_", "chooseWalletFile", function(path) {
                    form.wallet = null;
                    input.setValue(path);
                    if (!updateWallet(path)) {
                      input.markInvalid("Invalid wallet file.");
                    }
                  });
                }
              }
            ]
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Deploy",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        labelAlign: "top",
        items: [
          new Ext.form.ComboBox({
            fieldLabel: "Payer",
            tips:
              "Payer address used during deploy. Must be found in wallet file.",
            name: "deploy-payer",
            editable: false,
            forceSelection: true,
            triggerAction: "all",
            mode: "local",
            store: payerStore,
            valueField: "val",
            displayField: "label"
          }),
          {
            fieldLabel: "Gas Limit",
            tips: "Gas limit used during deploy.",
            name: "deploy-gas-limit",
            value: "30000000",
            validator: validateGas
          },
          {
            fieldLabel: "Gas Price",
            name: "deploy-gas-price",
            tips: "Gas price used during deploy.",
            validator: validateGas,
            value: "0"
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Invoke",
        labelAlign: "top",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        items: [
          new Ext.form.ComboBox({
            fieldLabel: "Payer",
            tips:
              "Payer address used during invoke. Must be found in wallet file.",
            name: "invoke-payer",
            editable: false,
            forceSelection: true,
            triggerAction: "all",
            mode: "local",
            store: payerStore,
            valueField: "val",
            displayField: "label"
          }),
          {
            fieldLabel: "Gas Limit",
            tips: "Gas limit used during invoke.",
            name: "invoke-gas-limit",
            value: "30000000",
            validator: validateGas
          },
          {
            fieldLabel: "Gas Price",
            name: "invoke-gas-price",
            tips: "Gas price used during invoke.",
            value: "0",
            validator: validateGas
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Network",
        labelAlign: "top",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        items: [
          new Ext.form.ComboBox({
            fieldLabel: "Type",
            name: "network-default",
            tips:
              "Specifies which network will be used during deploy and invoke.",
            editable: false,
            forceSelection: true,
            triggerAction: "all",
            mode: "local",
            store: networkStore,
            valueField: "val",
            displayField: "label"
          }),
          {
            fieldLabel: "Private",
            name: "network-private",
            tips:
              "PrivateNet address RPC address in the form <i>http://host:port</i> .",
            validator: function(v) {
              return kUrlRegexp.test(v);
            }
          }
        ]
      }
    ],

    buttons: [
      {
        text: "Save",
        onClick: function() {
          var form = formPanel.getForm();
          if (!form.isValid()) {
            showErr("Please fix error fields");
            return;
          }

          var values = form.getValues();
          var url = values["network-private"].match(kUrlRegexp);
          var scheme = url[1];
          var host = url[2];
          var port = url[3];
          values["network-private-host"] = scheme + "://" + host;
          values["network-private-port"] = parseInt(port);

          java.call("_settings_", "save", [values]);
        }
      },
      {
        text: "Cancel",
        onClick: function() {
          java.call("_settings_", "cancel");
        }
      }
    ],

    listeners: {
      afterrender: function() {
        loadConfig();
        loadNetwork();
      }
    }
  });

  formPanel.render(document.body);

  formPanel.getForm().items.each(function(field) {
    if (!field.tips) return;

    Ext.DomHelper.insertAfter(field.label.id, {
      tag: "div",
      class: "tips",
      html: field.tips
    });
  });
});
