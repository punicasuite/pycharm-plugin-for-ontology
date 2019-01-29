package com.hsiaosiyuan.idea.ont.module;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.punica.OntPunicaConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntInvokeConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.hsiaosiyuan.idea.ont.sdk.OntSdkSettings;
import com.hsiaosiyuan.idea.ont.webview.OntWebView;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OntProjectSettingsDialogWrap extends OntWebView {

  private Project project;
  private Settings settings;

  public OntProjectSettingsDialogWrap(Project project) {
    super();
    this.project = project;
    settings = new Settings();
  }

  @Override
  public String getTitle() {
    return "Project settings";
  }

  @Override
  public String getUrl() {
    URL url = this.getClass().getClassLoader().getResource("web/project/settings/index.html");
    assert url != null;
    return url.toString();
  }

  @Override
  public void onCreateScene(WebView webView, WebEngine webEngine) {
    webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        JSObject win = (JSObject) webEngine.executeScript("window");

        win.setMember("_ontSettings_", settings);
        System.out.println(win.getMember("loadNetwork"));
        win.call("loadNetwork", settings.getNetworkConfigs());
      }
    });
  }

  public class Settings {

    public void chooseFile(String title, String method) {
      FileChooserDescriptor descriptor = FileChooserDescriptorFactory
          .createSingleFileNoJarsDescriptor()
          .withTitle(title);

      SwingUtilities.invokeLater(() -> {
        FileChooser.chooseFile(descriptor, null, null, file -> {
          callJS(method, file.getPath());
        });
      });
    }

    public String getConfigJson() {
      JSONObject cfg = new JSONObject();
      try {
        cfg.put("wallet", OntPunicaConfig.getInstance(project).getWalletPath().toString());
      } catch (IOException e) {
        cfg.put("wallet", "");
      }

      JSONObject deploy = new JSONObject();
      try {
        OntDeployConfig deployConfig = OntDeployConfig.getInstance(project);
        deploy.put("gasLimit", deployConfig.gasLimit);
        deploy.put("gasPrice", deployConfig.gasPrice);
        deploy.put("payer", deployConfig.payer);
      } catch (IOException e) {
        e.printStackTrace();
      }
      cfg.put("deploy", deploy);

      JSONObject invoke = new JSONObject();
      try {
        OntInvokeConfig invokeConfig = OntInvokeConfig.getInstance(project);
        invoke.put("gasLimit", invokeConfig.gasLimit);
        invoke.put("gasPrice", invokeConfig.gasPrice);
        invoke.put("payer", invokeConfig.defaultPayer);
      } catch (IOException e) {
        e.printStackTrace();
      }
      cfg.put("invoke", invoke);

      JSONObject debug = new JSONObject();
      String ontDevPath = OntSdkSettings.getInstance().PUNICA_BIN;
      if (ontDevPath == null || ontDevPath.equals("")) {
        ontDevPath = OntPunica.getSuggestPath();
      }
      debug.put("ontdev", ontDevPath);
      cfg.put("debug", debug);

      return JSON.toJSONString(cfg);
    }

    // just return json data but does not check it's integrity
    // empty string indicates there are errors occur when reading json
    public String getWalletJson(String path) {
      Path filePath = Paths.get(path);
      File file = filePath.toFile();
      if (!file.exists()) return "";

      try {
        return new String(Files.readAllBytes(filePath));
      } catch (IOException e) {
        return "";
      }
    }

    public String getNetworkConfigs() {
      try {
        return new String(OntNetworkConfig.getInstance(project).getRaw());
      } catch (IOException e) {
        return "";
      }
    }

    public Boolean isOntdevValid(String path) {
      OntPunica punica = new OntPunica(path);
      return punica.exist();
    }

    public void cancel() {
      closeDialog();
    }

    public void save(JSObject cfg) {
      try {
        OntPunicaConfig punicaConfig = OntPunicaConfig.getInstance(project);
        punicaConfig.defaultWallet = (String) cfg.getMember("wallet-file");
        punicaConfig.save();

        OntNetworkConfig networkConfig = OntNetworkConfig.getInstance(project);
        networkConfig.defaultNet = (String) cfg.getMember("network-default");
        OntNetworkConfig.Network privateNet = networkConfig.networks.get("privateNet");
        privateNet.host = (String) cfg.getMember("network-private-host");
        privateNet.port = (int) cfg.getMember("network-private-port");
        networkConfig.save();

        OntDeployConfig deployConfig = OntDeployConfig.getInstance(project);
        deployConfig.gasLimit = Integer.valueOf((String) cfg.getMember("deploy-gas-limit"));
        deployConfig.gasPrice = Integer.valueOf((String) cfg.getMember("deploy-gas-price"));
        deployConfig.payer = (String) cfg.getMember("deploy-payer");
        deployConfig.save();

        OntInvokeConfig invokeConfig = OntInvokeConfig.getInstance(project);
        invokeConfig.gasLimit = Integer.valueOf((String) cfg.getMember("invoke-gas-limit"));
        invokeConfig.gasPrice = Integer.valueOf((String) cfg.getMember("invoke-gas-price"));
        invokeConfig.defaultPayer = (String) cfg.getMember("invoke-payer");
        invokeConfig.save();

        String ontdevPath = (String) cfg.getMember("ontdev-path");
        ontdevPath = ontdevPath.trim();
        if (!ontdevPath.equals("")) OntSdkSettings.getInstance().PUNICA_BIN = ontdevPath;

        closeDialog();
      } catch (Exception e) {
        e.printStackTrace();
        callJS("showErr", "Some errors occur, unable to save settings.");
      }
    }
  }
}
