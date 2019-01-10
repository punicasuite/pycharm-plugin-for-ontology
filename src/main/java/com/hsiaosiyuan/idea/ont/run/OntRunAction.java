package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.deploy.OntDeployConfigDialog;
import com.hsiaosiyuan.idea.ont.punica.OntPunicaConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
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

    if (!tryDoDeployIfNeeded(project)) return;


  }

  public boolean tryDoDeployIfNeeded(Project project) {
    OntNotifier notifier = OntNotifier.getInstance(project);
    OntDeployConfigDialog dialog;
    try {
      dialog = new OntDeployConfigDialog(project);
    } catch (IOException e1) {
      notifier.notifyError("Ontology", "Unable to load config: " + e1.getMessage());
      return false;
    }

    if (!dialog.showAndGet()) return false;

    try {
      String code = new String(Files.readAllBytes(Paths.get(AbiFile.srcPath2AvmPath(src)))).trim();
      OntDeployConfig.getInstance(project).deploy(code);
    } catch (Exception e) {
      notifier.notifyError("Ontology", "Failed to deploy contract: " + e.getMessage());
      return false;
    }

    return true;
  }
}
