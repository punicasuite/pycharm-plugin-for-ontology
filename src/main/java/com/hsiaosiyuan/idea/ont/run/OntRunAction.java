package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.deploy.OntDeployProcessHandler;
import com.hsiaosiyuan.idea.ont.invoke.OntInvokeProcessHandler;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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

    if (!isDeployed(project)) {
      deploy(project);
      return;
    }

    invoke(project);
  }

  private void deploy(Project project) {
    OntDeployProcessHandler handler = new OntDeployProcessHandler();
    ConsoleView consoleView = OntPunica.makeConsoleView(project, "Deploy");
    consoleView.attachToProcess(handler);
    handler.start(project, src);
  }

  private void invoke(Project project) {
    OntInvokeProcessHandler handler = new OntInvokeProcessHandler();
    ConsoleView consoleView = OntPunica.makeConsoleView(project, "Invoke");
    consoleView.attachToProcess(handler);
    handler.start(project, src, method);
  }

  private boolean isDeployed(Project project) {
    OntNotifier notifier = OntNotifier.getInstance(project);
    AbiFile abiFile = AbiIndexManager.getInstance().getAbi(src);
    OntNetworkConfig networkConfig;
    try {
      networkConfig = OntNetworkConfig.getInstance(project);
    } catch (IOException e) {
      notifier.notifyError("Ontology", "Unable to load config: " + e.getMessage());
      return false;
    }

    boolean isDeployed;
    try {
      isDeployed = OntPunica.isContractDeployed(networkConfig.getRpcAddr(), abiFile.hash);
    } catch (Exception e) {
      notifier.notifyError("Ontology", "Unable to check contract state: " + e.getMessage());
      return false;
    }

    return isDeployed;
  }
}
