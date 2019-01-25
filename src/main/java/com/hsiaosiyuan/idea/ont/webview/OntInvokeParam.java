package com.hsiaosiyuan.idea.ont.webview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.ontio.common.Address;
import com.github.ontio.common.Helper;
import com.github.ontio.sdk.exception.SDKException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntInvokeParam {
  public static Object parseValue(String value, OntInvokeParam.Type type) throws SDKException, OntInvokeParam.InvalidTypeException {
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
        return value.equals("true");
      case ARRAY:
        return parseArray(value);
      case MAP:
        return parseMap(value);
      default:
        throw new IllegalArgumentException("Unsupported type: " + type.label);
    }
  }

  public static ArrayList<Object> parseArray(String value) throws SDKException, OntInvokeParam.InvalidTypeException {
    ArrayList<Object> out = new ArrayList<>();
    List<OntInvokeParam.TypedItem> in = JSON.parseArray(value, OntInvokeParam.TypedItem.class);
    for (OntInvokeParam.TypedItem item : in) {
      Object t = parseValue(item.value.toString(), item.typ());
      out.add(t);
    }
    return out;
  }

  public static HashMap<String, Object> parseMap(String value) throws SDKException, OntInvokeParam.InvalidTypeException {
    HashMap<String, Object> out = new HashMap<>();
    Map<String, OntInvokeParam.TypedItem> in = JSON.parseObject(value, new TypeReference<Map<String, OntInvokeParam.TypedItem>>() {
    }.getType());
    for (Map.Entry<String, OntInvokeParam.TypedItem> entry : in.entrySet()) {
      Object t = parseValue(entry.getValue().value.toString(), entry.getValue().typ());
      out.put(entry.getKey(), t);
    }
    return out;
  }

  public static JSONObject parseRoot(String json) {
    return JSON.parseObject(json);
  }

  public static void convertDebugValue(org.json.JSONObject container, String name, String value, OntInvokeParam.Type type) {
    String kType = name + "-type";
    try {
      switch (type) {
        case STRING: {
          container.put(name, parseValue(value.trim(), OntInvokeParam.Type.STRING));
          container.put(kType, type.abiType());
          break;
        }
        case INTEGER: {
          container.put(name, parseValue(value.trim(), OntInvokeParam.Type.INTEGER));
          container.put(kType, type.abiType());
          break;
        }
        case ADDRESS: {
          container.put(name, parseValue(value.trim(), OntInvokeParam.Type.ADDRESS));
          container.put(kType, type.abiType());
          break;
        }
        case LONG: {
          container.put(name, parseValue(value.trim(), OntInvokeParam.Type.LONG));
          container.put(kType, type.abiType());
          break;
        }
        case BYTE_ARRAY: {
          container.put(name, parseValue(value.trim(), OntInvokeParam.Type.BYTE_ARRAY));
          container.put(kType, type.abiType());
          break;
        }
        case BOOLEAN: {
          container.put(name, value.trim());
          container.put(kType, type.abiType());
          break;
        }
        case ARRAY: {
          List<OntInvokeParam.TypedItem> in = JSON.parseArray(value.trim(), OntInvokeParam.TypedItem.class);
          container.put(kType, type.abiType());
          for (int i = 0; i < in.size(); i++) {
            OntInvokeParam.TypedItem item = in.get(i);
            convertDebugValue(container, name + "[" + i + "]", item.value.toString(), item.typ());
          }
          break;
        }
        case MAP: {
          Map<String, OntInvokeParam.TypedItem> in = JSON.parseObject(value, new TypeReference<Map<String, OntInvokeParam.TypedItem>>() {
          }.getType());
          container.put(kType, type.abiType());

          List<Map.Entry<String, OntInvokeParam.TypedItem>> items = new ArrayList<>(in.entrySet());
          for (int i = 0; i < items.size(); i++) {
            Map.Entry<String, OntInvokeParam.TypedItem> item = items.get(i);
            String key = item.getKey();
            OntInvokeParam.TypedItem val = item.getValue();

            convertDebugValue(container, name + "[" + i + "]-name", key, OntInvokeParam.Type.STRING);
            convertDebugValue(container, name + "[" + i + "]", val.value.toString(), val.typ());
          }
          break;
        }
        default:
          throw new OntInvokeParam.InvalidTypeException("Unsupported type");
      }
    } catch (Exception e) {
      e.printStackTrace();
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
    public String abiType() throws OntInvokeParam.InvalidTypeException {
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
          throw new OntInvokeParam.InvalidTypeException("Unsupported label");
      }
    }

    public static OntInvokeParam.Type fomLabel(String label) throws OntInvokeParam.InvalidTypeException {
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
          throw new OntInvokeParam.InvalidTypeException("Unsupported label");
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
      if (!(obj instanceof Item)) return super.equals(obj);

      return ((Item) obj).getId().equals(id);
    }
  }

  public static class TypedItem {
    public String type;
    public Object value;
    public String name;

    public OntInvokeParam.Type typ() throws OntInvokeParam.InvalidTypeException {
      return OntInvokeParam.Type.fomLabel(type);
    }
  }

  public static class InvalidTypeException extends Exception {
    public InvalidTypeException(String msg) {
      super(msg);
    }
  }
}
