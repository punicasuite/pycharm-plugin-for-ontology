package com.hsiaosiyuan.idea.ont.run;

import com.alibaba.fastjson.JSONObject;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.deploy.OntDeployConfigDialog;
import com.hsiaosiyuan.idea.ont.invoke.OntInvokeDialog;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OntRunAction extends AnAction {
  private String src;
  private String method;

  public OntRunAction(String src, String method) {
    super();
    this.src = src;
    this.method = method;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    int state = tryDoDeployIfNeeded(project);
    if (state <= 0) return;

    OntNotifier notifier = OntNotifier.getInstance(project);

    OntInvokeDialog invokeDialog;
    try {
      invokeDialog = new OntInvokeDialog(project, src, method);
    } catch (Exception e1) {
      notifier.notifyError("Ontology", e1);
      return;
    }

    if (!invokeDialog.showAndGet()) return;

    AbiFunction fn;
    try {
      fn = invokeDialog.getFn();
    } catch (Exception e1) {
      notifier.notifyError("Ontology", e1);
      return;
    }

    AbiFile abiFile = AbiIndexManager.getInstance().src2abi.get(src);
    try {
      // TODO:: run in background progress and display exec result into the bottom tool window
      JSONObject resp = (JSONObject) OntDeployConfig.getInstance(project).invoke(abiFile.hash, fn, true, false);
      System.out.println(resp);
    } catch (Exception e1) {
      notifier.notifyError("Ontology", e1);
    }
  }

  public int tryDoDeployIfNeeded(Project project) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    AbiFile abiFile = AbiIndexManager.getInstance().src2abi.get(src);
    OntNetworkConfig networkConfig;
    try {
      networkConfig = OntNetworkConfig.getInstance(project);
    } catch (IOException e) {
      notifier.notifyError("Ontology", "Unable to load config: " + e.getMessage());
      return -1;
    }

    boolean isDeployed;
    try {
      isDeployed = OntPunica.isContractDeployed(networkConfig.getRpcAddr(), abiFile.hash);
    } catch (Exception e) {
      notifier.notifyError("Ontology", "Unable to check contract state: " + e.getMessage());
      return -1;
    }

    if (isDeployed) return 1;

    OntDeployConfigDialog dialog;
    try {
      Path path = Paths.get(src);
      String filename = path.getFileName().toString();
      filename = filename.substring(0, filename.length() - 3);
      dialog = new OntDeployConfigDialog(project, filename);
    } catch (IOException e1) {
      notifier.notifyError("Ontology", "Unable to load config: " + e1.getMessage());
      return -1;
    }

    if (!dialog.showAndGet()) return 0;

    try {
      String code = new String(Files.readAllBytes(Paths.get(AbiFile.srcPath2AvmPath(src)))).trim();
      OntDeployConfig.getInstance(project).deploy(code);
      notifier.notifySuccess("Ontology", "Contract deployed successfully.");
    } catch (Exception e) {
      notifier.notifyError("Ontology", "Failed to deploy contract: " + e.getMessage());
      return -1;
    }

    return 1;
  }
}
