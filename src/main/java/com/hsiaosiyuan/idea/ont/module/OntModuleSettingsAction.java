package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.hsiaosiyuan.idea.ont.webview.OntProjSettingsDialogWrap;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

public class OntModuleSettingsAction extends AnAction {

  private void disable(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
    e.getPresentation().setVisible(false);
  }

  private void enable(@NotNull AnActionEvent e) {
    e.getPresentation().setIcon(OntIcons.ICON);
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    Project project = e.getProject();
    if (project == null) {
      disable(e);
      return;
    }

    String bashDir = project.getBasePath();
    if (bashDir == null) {
      disable(e);
      return;
    }

    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file == null) {
      disable(e);
      return;
    }

    if (file.getPath().equals(bashDir)) {
      enable(e);
    } else {
      disable(e);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    OntProjSettingsDialogWrap dialog = new OntProjSettingsDialogWrap(e.getProject());
    dialog.showAndWait();
  }
}
