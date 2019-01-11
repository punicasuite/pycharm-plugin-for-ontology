package com.hsiaosiyuan.idea.ont.invoke;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.ontio.common.Address;
import com.github.ontio.common.Helper;
import com.github.ontio.sdk.exception.SDKException;
import com.hsiaosiyuan.idea.ont.deploy.OntDeployConfigDialog;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntFnParameter implements ItemListener {
  private JPanel panel;

  private JComboBox cbVarType;
  private JTextField txVal;
  private JLabel txVarName;
  private JTextArea taVal;
  private JPanel panValue;
  private JRadioButton rbTrue;
  private JRadioButton rbFalse;
  private JScrollPane sp;

  private String vName;
  private Type vType;

  private WeakReference<JComponent> parent;

  @SuppressWarnings("unchecked")
  public OntFnParameter(JComponent container, String paramName) {
    parent = new WeakReference<>(container);

    vName = paramName;
    vType = Type.STRING;

    txVarName.setText(paramName);

    for (Type t : Type.values()) {
      cbVarType.addItem(new Item(t.label, t.label));
    }
    cbVarType.addItemListener(this);

    ButtonGroup groupTrueFalse = new ButtonGroup();
    groupTrueFalse.add(rbTrue);
    groupTrueFalse.add(rbFalse);

    updateStyle();
  }

  public JComponent getComponent() {
    return panel;
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      Item item = (Item) e.getItem();
      try {
        vType = Type.fomLabel(item.getId());
      } catch (InvalidTypeException e1) {
        e1.printStackTrace();
      }
      updateStyle();
    }
  }

  private void hideInputs() {
    txVal.setVisible(false);

    sp.setVisible(false);
    taVal.setVisible(false);

    rbTrue.setVisible(false);
    rbFalse.setVisible(false);

    sp.getParent().revalidate();
    panel.revalidate();
    parent.get().revalidate();
  }

  private void showInputs() {
    switch (vType) {
      case STRING:
      case INTEGER:
      case ADDRESS:
      case LONG:
      case BYTE_ARRAY: {
        txVal.setVisible(true);
        break;
      }
      case BOOLEAN: {
        rbTrue.setVisible(true);
        rbFalse.setVisible(true);
        break;
      }
      case ARRAY:
      case MAP: {
        sp.setVisible(true);
        taVal.setVisible(true);
        break;
      }
    }
  }

  private void updateStyle() {
    hideInputs();
    showInputs();
  }

  public JComponent asLastField() {
    panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    return panel;
  }

  public static Object parseValue(String value, Type type) throws SDKException, InvalidTypeException {
    switch (type) {
      case STRING:
        return value;
      case INTEGER:
        return Long.valueOf(value);
      case ADDRESS:
        return Address.decodeBase58(value).toArray();
      case LONG:
        return Long.valueOf(value);
      case BYTE_ARRAY:
        return Helper.hexToBytes(value);
      case BOOLEAN:
        return value.equals("True");
      case ARRAY:
        return parseArray(value);
      case MAP:
        return parseMap(value);
      default:
        throw new IllegalArgumentException("Unsupported type: " + type.label);
    }
  }

  public static ArrayList<Object> parseArray(String value) throws SDKException, InvalidTypeException {
    ArrayList<Object> out = new ArrayList<>();
    List<TypedItem> in = JSON.parseArray(value, TypedItem.class);
    for (TypedItem item : in) {
      Object t = parseValue(item.value.toString(), item.typ());
      out.add(t);
    }
    return out;
  }

  public static HashMap<String, Object> parseMap(String value) throws SDKException, InvalidTypeException {
    HashMap<String, Object> out = new HashMap<>();
    Map<String, TypedItem> in = JSON.parseObject(value, new TypeReference<Map<String, TypedItem>>() {
    }.getType());
    for (Map.Entry<String, TypedItem> entry : in.entrySet()) {
      Object t = parseValue(entry.getValue().value.toString(), entry.getValue().typ());
      out.put(entry.getKey(), t);
    }
    return out;
  }

  public Type getType() {
    return vType;
  }

  public Object getValue() throws SDKException, InvalidTypeException {
    switch (vType) {
      case STRING:
        return parseValue(txVal.getText().trim(), Type.STRING);
      case INTEGER:
        return parseValue(txVal.getText().trim(), Type.INTEGER);
      case ADDRESS:
        return parseValue(txVal.getText().trim(), Type.ADDRESS);
      case LONG:
        return parseValue(txVal.getText().trim(), Type.LONG);
      case BYTE_ARRAY:
        return parseValue(txVal.getText().trim(), Type.BYTE_ARRAY);
      case BOOLEAN:
        return rbTrue.isSelected();
      case ARRAY:
        return parseArray(taVal.getText().trim());
      case MAP: {
        return parseMap(taVal.getText().trim());
      }
      default:
        throw new InvalidTypeException("Unsupported label");
    }
  }

  public enum Type {
    STRING("String"),
    INTEGER("Integer"),
    LONG("Long"),
    BOOLEAN("Boolean"),
    BYTE_ARRAY("ByteArray"),
    ADDRESS("Address"),
    MAP("Map"),
    ARRAY("Array");

    private String label;

    Type(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    // return type strings which are being supported in:
    // com.github.ontio.smartcontract.neovm.abi::setValue
    public String abiType() throws InvalidTypeException {
      switch (this) {
        case STRING:
          return "String";
        case INTEGER:
        case LONG:
          return "Integer";
        case ADDRESS:
        case BYTE_ARRAY:
          return "ByteArray";
        case BOOLEAN:
          return "Boolean";
        case ARRAY:
          return "Array";
        case MAP: {
          return "Map";
        }
        default:
          throw new InvalidTypeException("Unsupported label");
      }
    }

    public static Type fomLabel(String label) throws InvalidTypeException {
      switch (label) {
        case "String":
          return STRING;
        case "Integer":
          return INTEGER;
        case "Long":
          return LONG;
        case "Boolean":
          return BOOLEAN;
        case "ByteArray":
          return BYTE_ARRAY;
        case "Address":
          return ADDRESS;
        case "Map":
          return MAP;
        case "Array":
          return ARRAY;
        default:
          throw new InvalidTypeException("Unsupported label");
      }
    }
  }

  public static class Item {
    private String id;
    private String description;

    public Item(String id, String description) {
      this.id = id;
      this.description = description;
    }

    public String getId() {
      return id;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof OntDeployConfigDialog.Item)) return super.equals(obj);

      return ((OntDeployConfigDialog.Item) obj).getId().equals(id);
    }
  }

  public static class TypedItem {
    public String type;
    public Object value;

    public Type typ() throws InvalidTypeException {
      return Type.fomLabel(type);
    }
  }

  public static class InvalidTypeException extends Exception {
    public InvalidTypeException(String msg) {
      super(msg);
    }
  }
}
