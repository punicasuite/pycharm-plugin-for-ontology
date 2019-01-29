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

  var getField = function(name) {
    return formPanel.getForm().findField(name);
  };

  var formPanel = new Ext.FormPanel({
    labelWidth: 75,
    bodyStyle: "padding:5px 5px 0",
    autoHeight: true,
    width: Ext.getBody().getViewSize().width - getScrollbarWidth(),

    items: [
      {
        xtype: "fieldset",
        title: "String conversion",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        labelAlign: "top",
        items: [
          {
            fieldLabel: "Hex String",
            name: "str-conv-hex"
          },
          {
            fieldLabel: "String",
            name: "str-conv-str"
          },
          {
            xtype: "compositefield",
            hideLabel: true,
            items: [
              {
                xtype: "textfield",
                hidden: true,
                fieldLabel: "String",
                name: "str-conv-btn-holder"
              },
              {
                xtype: "button",
                name: "btn-hex-str",
                text: "Hex string => String",
                onClick: function() {
                  var hex = getField("str-conv-hex").getValue();
                  var str = java.call("_ontConvertor_", "hexStrToStr", [hex]);
                  getField("str-conv-str").setValue(str);
                }
              },
              {
                xtype: "button",
                name: "btn-str-hex",
                text: "String => Hex string",
                onClick: function() {
                  var str = getField("str-conv-str").getValue();
                  var hex = java.call("_ontConvertor_", "strToHexStr", [str]);
                  getField("str-conv-hex").setValue(hex);
                }
              }
            ]
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Address conversion",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        labelAlign: "top",
        items: [
          {
            fieldLabel: "Script hash",
            name: "addr-conv-hash"
          },
          {
            fieldLabel: "Address",
            name: "addr-conv-addr"
          },
          {
            xtype: "compositefield",
            hideLabel: true,
            items: [
              {
                xtype: "textfield",
                hidden: true,
                fieldLabel: "String",
                name: "add-conv-btn-holder"
              },
              {
                xtype: "button",
                name: "btn-hash-addr",
                text: "Script hash => Address",
                onClick: function() {
                  var hash = getField("addr-conv-hash").getValue();
                  var addr = java.call("_ontConvertor_", "scriptHashToAddress", [
                    hash
                  ]);
                  getField("addr-conv-addr").setValue(addr);
                }
              },
              {
                xtype: "button",
                name: "btn-addr-hash",
                text: "Address => Script hash",
                onClick: function() {
                  var addr = getField("addr-conv-addr").getValue();
                  var hash = java.call("_ontConvertor_", "addressToScriptHash", [
                    addr
                  ]);
                  getField("addr-conv-hash").setValue(hash);
                }
              }
            ]
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Number conversion",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        labelAlign: "top",
        items: [
          {
            fieldLabel: "Hex String",
            name: "num-conv-hex"
          },
          {
            fieldLabel: "Number",
            name: "num-conv-num"
          },
          {
            xtype: "compositefield",
            hideLabel: true,
            items: [
              {
                xtype: "textfield",
                hidden: true,
                fieldLabel: "String",
                name: "num-conv-btn-holder"
              },
              {
                xtype: "button",
                name: "btn-hex-num",
                text: "Hex String => Number",
                onClick: function() {
                  var hex = getField("num-conv-hex").getValue();
                  var num = java.call("_ontConvertor_", "hexStrToNumber", [hex]);
                  getField("num-conv-num").setValue(num);
                }
              },
              {
                xtype: "button",
                name: "btn-num-hex",
                text: "Number => Hex String",
                onClick: function() {
                  var num = getField("num-conv-num").getValue();
                  var hex = java.call("_ontConvertor_", "numberToHexStr", [num]);
                  getField("num-conv-hex").setValue(hex);
                }
              }
            ]
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Endian conversion",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        labelAlign: "top",
        items: [
          {
            fieldLabel: "BigEndian hex string",
            name: "endian-conv-be"
          },
          {
            fieldLabel: "LittleEndian hex string",
            name: "endian-conv-le"
          },
          {
            xtype: "compositefield",
            hideLabel: true,
            items: [
              {
                xtype: "textfield",
                hidden: true,
                fieldLabel: "String",
                name: "endian-conv-btn-holder"
              },
              {
                xtype: "button",
                name: "btn-be-le",
                text: "BigEndian => LittleEndian",
                onClick: function() {
                  var be = getField("endian-conv-be").getValue();
                  var le = java.call("_ontConvertor_", "reverseBytes", [be]);
                  getField("endian-conv-le").setValue(le);
                }
              },
              {
                xtype: "button",
                name: "btn-le-be",
                text: "LittleEndian => BigEndian",
                onClick: function() {
                  var le = getField("endian-conv-le").getValue();
                  var be = java.call("_ontConvertor_", "reverseBytes", [le]);
                  getField("endian-conv-be").setValue(be);
                }
              }
            ]
          }
        ]
      },
      {
        xtype: "fieldset",
        title: "Byte array conversion",
        collapsible: true,
        autoHeight: true,
        defaults: { width: 280 },
        defaultType: "textfield",
        labelAlign: "top",
        items: [
          {
            fieldLabel: "Hex String",
            name: "bytearray-conv-hex"
          },
          {
            fieldLabel: "Byte array",
            name: "bytearray-conv-ba"
          },
          {
            xtype: "compositefield",
            hideLabel: true,
            items: [
              {
                xtype: "textfield",
                hidden: true,
                fieldLabel: "String",
                name: "bytearray-conv-btn-holder"
              },
              {
                xtype: "button",
                name: "btn-hex-ba",
                text: "Hex string => Byte array",
                onClick: function() {
                  var hex = getField("bytearray-conv-hex").getValue();
                  var ba = java.call("_ontConvertor_", "hexStrToBa", [hex]);
                  getField("bytearray-conv-ba").setValue(ba);
                }
              },
              {
                xtype: "button",
                name: "btn-ba-hex",
                text: "Byte array => Hex string",
                onClick: function() {
                  var ba = getField("bytearray-conv-ba").getValue();
                  var hex = java.call("_ontConvertor_", "baToHexStr", [ba]);
                  getField("bytearray-conv-hex").setValue(hex);
                }
              }
            ]
          }
        ]
      }
    ]
  });

  formPanel.render(document.body);
});
