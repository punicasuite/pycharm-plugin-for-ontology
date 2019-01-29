package com.hsiaosiyuan.idea.ont.deploy;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class OntDeployAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file == null || !file.getPath().endsWith(".avm")) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setVisible(false);
    } else {
      e.getPresentation().setIcon(OntIcons.ICON);
      e.getPresentation().setEnabledAndVisible(true);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file == null) return;

    Project project = e.getProject();
    OntDeployProcessHandler handler = new OntDeployProcessHandler();
    ConsoleView consoleView = OntPunica.makeConsoleView(project, "Deploy");
    consoleView.attachToProcess(handler);
    handler.start(project, file.getPath());
  }
}
