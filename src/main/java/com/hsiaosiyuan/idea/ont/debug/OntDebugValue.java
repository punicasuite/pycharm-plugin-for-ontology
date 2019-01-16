package com.hsiaosiyuan.idea.ont.debug;

import com.github.ontio.common.Helper;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.xdebugger.frame.*;
import com.intellij.xdebugger.frame.presentation.XNumericValuePresentation;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;

public class OntDebugValue extends XValue {
  private JSONObject myVariable;
  private OntDebugAgent myAgent;
  private String myValue;
  private String myTypeName;
  private String myRf;

  private boolean isByteArray;
  private byte[] bytes;

  public OntDebugValue(JSONObject variable, OntDebugAgent agent) {
    myVariable = variable;
    myAgent = agent;
    try {
      myValue = variable.getString("value");
      myTypeName = variable.getString("type");
      myRf = variable.getString("variablesReference");

      if (myTypeName.equals("string")) {
        isByteArray = true;
        if (myValue.startsWith("0x")) {
          String v = myValue.substring(2);
          bytes = Helper.hexToBytes(v);
          if (checkUtf8(bytes)) {
            isByteArray = false;
            myValue = new String(bytes);
          }
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public static boolean checkUtf8(byte[] barr) {
    CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
    ByteBuffer buf = ByteBuffer.wrap(barr);
    try {
      decoder.decode(buf);
    } catch (CharacterCodingException e) {
      return false;
    }
    return true;
  }

  @Override
  public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
    XValuePresentation presentation = getPresentation();
    node.setPresentation(null, presentation, isMap() || isArray());
  }

  public boolean isString() {
    return myTypeName.equals("string") && !isByteArray;
  }

  public boolean isByteArray() {
    return myTypeName.equals("string") && isByteArray;
  }

  public boolean isNumber() {
    return myTypeName.equals("number");
  }

  public boolean isBool() {
    return myTypeName.equals("bool");
  }

  public boolean isMap() {
    return myTypeName.equals("map");
  }

  public boolean isArray() {
    return myTypeName.equals("array");
  }

  @NotNull
  private XValuePresentation getPresentation() {
    final String stringValue = myValue;
    if (isNumber()) return new XNumericValuePresentation(stringValue);
    if (isString()) return new XStringValuePresentation(stringValue);
    if (isByteArray()) return new XStringValuePresentation("") {
      @Override
      public void renderValue(@NotNull XValueTextRenderer renderer) {
        ArrayList<String> b = new ArrayList<>();
        for (byte aByte : bytes) {
          b.add(String.format("%02x ", aByte));
        }
        renderer.renderValue("{bytearray} ", DefaultLanguageHighlighterColors.DOC_COMMENT);
        renderer.renderValue("[ " + StringUtil.join(b, ", ") + " ]");
      }
    };
    if (isBool()) {
      return new XStringValuePresentation("") {
        @Override
        public void renderValue(@NotNull XValueTextRenderer renderer) {
          renderer.renderValue("{bool} ", DefaultLanguageHighlighterColors.DOC_COMMENT);
          renderer.renderValue(myValue);
        }
      };
    }
    return new XRegularValuePresentation(myValue, myTypeName);
  }

  @SuppressWarnings("Duplicates")
  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    myAgent.queryVariables(myRf).thenAcceptAsync((resp) -> {
      try {
        JSONArray vs = resp.getJSONArray("variables");
        final XValueChildrenList xValues = new XValueChildrenList();

        for (int i = 0; i < vs.length(); i++) {
          JSONObject v = vs.getJSONObject(i);
          xValues.add(v.getString("name"), new OntDebugValue(v, myAgent));
        }

        node.addChildren(xValues, true);
      } catch (Exception e) {
        e.printStackTrace();
      }

    });
  }
}
