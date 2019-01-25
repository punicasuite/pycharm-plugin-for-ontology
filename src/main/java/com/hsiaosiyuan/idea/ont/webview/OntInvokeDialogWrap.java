package com.hsiaosiyuan.idea.ont.webview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.github.ontio.smartcontract.neovm.abi.Parameter;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class OntInvokeDialogWrap extends OntWebView {
  private String srcPath;
  private String methodName;
  private String title;

  private Invocation invocation;

  public OntInvokeDialogWrap(String src, String method) {
    super();

    srcPath = src;
    methodName = method;
    title = "Invoke: " + AbiFile.extractSrcFilename(src) + "::" + method;

    invocation = new Invocation();
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getUrl() {
    URL url = this.getClass().getClassLoader().getResource("web/invoke/index.html");
    assert url != null;
    return url.toString();
  }

  @Override
  public void onCreateScene(WebView webView, WebEngine webEngine) {
    webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        JSObject win = (JSObject) webEngine.executeScript("window");
        win.setMember("invocation", invocation);
        win.setMember("_params_", makeParameters());
        webEngine.executeScript("setupParams()");
      }
    });
  }

  private AbiFunction getAbiFn() throws Exception {
    AbiFile abiFile = AbiIndexManager.getInstance().getAbi(srcPath);
    if (abiFile == null) {
      throw new Exception("Missing ABI file: " + srcPath);
    }
    AbiFunction fn = abiFile.getFn(methodName);
    if (fn == null) {
      throw new Exception("Unrecognized method: " + methodName + " in via ABI: " + srcPath);
    }
    return fn;
  }

  private String makeParameters() {
    try {
      AbiFunction fn = getAbiFn();

      JSONObject root = new JSONObject();
      root.put("name", "Parameters");
      root.put("type", "Map");

      JSONObject rootValue = new JSONObject();
      root.put("value", rootValue);

      fn.parameters.forEach(p -> {
        JSONObject pv = new JSONObject();
        pv.put("type", "String");
        pv.put("value", "");
        rootValue.put(p.name, pv);
      });

      return JSON.toJSONString(root);
    } catch (Exception e) {
      e.printStackTrace();
      return "{\"type\":\"Map\",\"name\":\"Parameters\",\"value\":{}}";
    }
  }

  public Boolean ok() {
    return invocation.doDebugFlag || invocation.doInvokeFlag;
  }

  public Boolean isInvokeMode() {
    return invocation.doInvokeFlag;
  }

  // for invocation
  public AbiFunction getFn() throws Exception {
    AbiFunction fn = getAbiFn();

    String rootParamStr = invocation.params;
    assert rootParamStr != null;

    JSONObject rootParam = OntInvokeParam.parseRoot(rootParamStr);
    String paramsStr = rootParam.getJSONObject("value").toJSONString();
    JSONObject paramsDict = rootParam.getJSONObject("value");
    HashMap<String, Object> parameters = OntInvokeParam.parseMap(paramsStr);

    AbiFunction fun = new AbiFunction();
    fun.name = fn.name;
    fun.parameters = new ArrayList<>();
    for (Parameter param : fn.parameters) {
      Parameter tp = new Parameter();
      tp.name = param.name;

      Object v = parameters.get(param.name);
      tp.type = paramsDict.getJSONObject(param.name).getString("type");
      tp.setValue(v);

      fun.parameters.add(tp);
    }
    return fun;
  }

  public Boolean getPreExec() {
    return invocation.preExec;
  }

  // contract params for debugging
  public org.json.JSONObject getDebugParams() throws Exception {
    AbiFunction fn = getAbiFn();

    String rootParamStr = invocation.params;
    assert rootParamStr != null;

    JSONObject rootParam = OntInvokeParam.parseRoot(rootParamStr);
    JSONObject params = rootParam.getJSONObject("value");

    org.json.JSONObject ret = new org.json.JSONObject();

    for (Parameter param : fn.parameters) {
      JSONObject paramData = params.getJSONObject(param.name);
      String name = param.name;
      String type = paramData.getString("type");
      String value = paramData.get("value").toString();

      OntInvokeParam.convertDebugValue(ret, name, value, OntInvokeParam.Type.fomLabel(type));
    }

    return ret;
  }

  public class Invocation {
    public Boolean doInvokeFlag = false;
    public Boolean doDebugFlag = false;
    public Boolean preExec = false;
    public String params = null;

    public void done() {
      closeDialog();
    }

    public void invoke(String params, Boolean preExec) {
      this.params = params;
      this.doInvokeFlag = true;
      this.preExec = preExec;
      System.out.println(params);
      System.out.println(preExec);
      done();
    }

    public void debug(String params) {
      this.params = params;
      this.doDebugFlag = true;
      done();
    }
  }
}
