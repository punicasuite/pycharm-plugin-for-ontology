package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.hsiaosiyuan.idea.ont.punica.OntCompileProcessHandler;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
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
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (file == null || !file.getPath().endsWith(".py")) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setVisible(false);
    } else {
      e.getPresentation().setIcon(OntIcons.ICON);
      e.getPresentation().setEnabledAndVisible(true);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
    if (file == null) return;

    Project project = e.getProject();
    if (project == null) return;

    OntCompileProcessHandler compileProcess = new OntCompileProcessHandler();

    compileProcess.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        super.processTerminated(event);
        ApplicationManager.getApplication().invokeLater(() -> {
          VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        });
      }
    });

    OntPunica.startProcess(compileProcess, project);
    compileProcess.start(project, file.getPath());
  }
}
