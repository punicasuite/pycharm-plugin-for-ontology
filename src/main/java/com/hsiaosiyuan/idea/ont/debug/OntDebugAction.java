package com.hsiaosiyuan.idea.ont.debug;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.intellij.execution.ExecutionException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

public class OntDebugAction extends AnAction {
  private String mySrc;
  private String myMethod;

  public OntDebugAction(String src, String method) {
    super();

    mySrc = src;
    myMethod = method;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setIcon(AllIcons.Actions.StartDebugger);

    String title = "Debug: " + AbiFile.extractSrcFilename(mySrc) + "::" + myMethod;
    e.getPresentation().setText(title, false);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    OntNotifier notifier = OntNotifier.getInstance(project);
    try {
      XDebuggerManager.getInstance(project).startSessionAndShowTab(
          "Ontology Debugger",
          OntIcons.ICON,
          null,
          false,
          new XDebugProcessStarter() {
            @NotNull
            @Override
            public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
              return new OntDebugProcess(session, mySrc, myMethod);
            }
          });
    } catch (Exception e1) {
      notifier.notifyError("Ontology", e1);
    }
  }
}
