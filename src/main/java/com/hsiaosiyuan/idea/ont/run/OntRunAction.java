package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.deploy.OntDeployConfigDialog;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.hsiaosiyuan.idea.ont.ui.OntNotifier;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OntRunAction extends AnAction { 
  private String src;

  public OntRunAction(String src) {
    super();
    this.src = src;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    int state = tryDoDeployIfNeeded(project);
    if (state <= 0) return;

    System.out.println("Invoke");
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
      dialog = new OntDeployConfigDialog(project);
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
