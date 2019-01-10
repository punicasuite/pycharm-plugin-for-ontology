package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.punica.OntPunicaFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

public class OntCompileAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setIcon(OntIcons.ICON);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
    if (file == null) return;

    Project project = e.getProject();
    if (project == null) return;

    GeneralCommandLine cmd = OntPunicaFactory.create().makeCompileCmd(project.getBasePath(), file.getPath());

    OntPunica.startCmdProcess(cmd, project, evt -> {
      ApplicationManager.getApplication().invokeLater(() -> {
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
      });
    });
  }
}
