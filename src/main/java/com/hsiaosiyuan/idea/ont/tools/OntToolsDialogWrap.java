package com.hsiaosiyuan.idea.ont.tools;

import com.github.ontio.common.Address;
import com.github.ontio.common.Helper;
import com.hsiaosiyuan.idea.ont.webview.OntWebView;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.math.BigInteger;
import java.net.URL;

public class OntToolsDialogWrap extends OntWebView {
  @Override
  public String getTitle() {
    return "Ontology tools";
  }

  @Override
  public String getUrl() {
    URL url = this.getClass().getClassLoader().getResource("web/project/tools/index.html");
    assert url != null;
    return url.toString();
  }

  @Override
  public void onCreateScene(WebView webView, WebEngine webEngine) {
    webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        JSObject win = (JSObject) webEngine.executeScript("window");
        win.setMember("_ontConvertor_", new Convertor());
      }
    });
  }

  public class Convertor {
    public String hexStrToStr(String hexStr) {
      try {
        byte[] bytes = Helper.hexToBytes(hexStr);
        return new String(bytes);
      } catch (Exception e) {
        return "";
      }
    }

    public String strToHexStr(String str) {
      try {
        return Helper.toHexString(str.getBytes());
      } catch (Exception e) {
        return "";
      }
    }

    public String scriptHashToAddress(String hash) {
      try {
        Address address = Address.parse(hash);
        return address.toBase58();
      } catch (Exception e) {
        return "";
      }
    }

    public String addressToScriptHash(String address) {
      try {
        Address addr = Address.decodeBase58(address);
        return Helper.toHexString(addr.toArray());
      } catch (Exception e) {
        return "";
      }
    }

    public String hexStrToNumber(String hexStr) {
      if (hexStr.startsWith("0x")) hexStr = hexStr.substring(2);
      try {
        BigInteger integer = new BigInteger(Helper.reverse(hexStr), 16);
        return integer.toString(10);
      } catch (Exception e) {
        return "";
      }
    }

    public String numberToHexStr(String number) {
      try {
        BigInteger integer = new BigInteger(number, 10);
        return Helper.toHexString(Helper.reverse(integer.toByteArray()));
      } catch (Exception e) {
        return "";
      }
    }

    public String reverseBytes(String val) {
      try {
        if (val.startsWith("0x")) val = val.substring(2);
        return Helper.reverse(val);
      } catch (Exception e) {
        return "";
      }
    }

    public String baToHexStr(String ba) {
      try {
        String hex = ba.replaceAll("(\\{|\\}|0x|0X|,|\\s)", "");
        return Helper.toHexString(Helper.hexToBytes(hex));
      } catch (Exception e) {
        return "";
      }
    }

    public String hexStrToBa(String hex) {
      try {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        byte[] bytes = Helper.hexToBytes(hex);
        for (int i = 0; i < bytes.length; i++) {
          sb.append("0x");
          int v = Byte.toUnsignedInt(bytes[i]);
          sb.append(Integer.toHexString(v >>> 4));
          sb.append(Integer.toHexString(v & 0x0f));
          if (i != bytes.length - 1) {
            sb.append(", ");
          }
        }
        sb.append("}");
        return sb.toString();
      } catch (Exception e) {
        return "";
      }
    }
  }
}
