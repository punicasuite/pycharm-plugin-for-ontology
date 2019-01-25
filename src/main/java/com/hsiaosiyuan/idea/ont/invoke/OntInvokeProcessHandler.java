package com.hsiaosiyuan.idea.ont.invoke;

import com.alibaba.fastjson.JSON;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.hsiaosiyuan.idea.ont.OntIcons;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.debug.OntDebugProcess;
import com.hsiaosiyuan.idea.ont.punica.config.OntInvokeConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.hsiaosiyuan.idea.ont.run.OntProcessHandler;
import com.hsiaosiyuan.idea.ont.webview.OntInvokeDialogWrap;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.OutputStream;

public class OntInvokeProcessHandler extends OntProcessHandler {

  @Nullable
  @Override
  public OutputStream getProcessInput() {
    return null;
  }

  public void start(final Project project, final String src, final String method) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    String filename = AbiFile.extractSrcFilename(src);

    notifyTextAvailableWithTimestamp("Invoke contract: " + filename + "::" + method, ProcessOutputTypes.SYSTEM);
    notifyTextAvailableWithTimestamp("Waiting for user's interaction...", ProcessOutputTypes.SYSTEM);

    OntInvokeDialogWrap dialog = new OntInvokeDialogWrap(src, method);
    dialog.showAndWait();

    if (!dialog.ok()) {
      notifyProcessTerminated(1);
      notifyTextAvailableWithTimestamp("User canceled", ProcessOutputTypes.STDERR);
      return;
    }

    final AbiFunction fn;
    try {
      fn = dialog.getFn();
    } catch (Exception e) {
      notifyProcessTerminated(1);
      notifier.notifyError("Ontology", e);
      return;
    }

    if (dialog.isInvokeMode()) {
      doInvoke(project, src, fn, dialog.getPreExec());
    } else {
      try {
        JSONObject params = dialog.getDebugParams();
        doDebug(project, src, method, params);
      } catch (Exception e) {
        notifier.notifyError("Ontology", e);
      }
    }
  }

  private void doInvoke(Project project, String src, AbiFunction fn, Boolean preExec) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    final OntInvokeProcessHandler handler = this;
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Deploy") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);

        AbiFile abiFile = AbiIndexManager.getInstance().getAbi(src);
        try {
          OntNetworkConfig networkConfig = OntNetworkConfig.getInstance(project);
          handler.notifyTextAvailableWithTimestamp("Sending to " + networkConfig.getRpcAddr(), ProcessOutputTypes.SYSTEM);

          Object resp = OntInvokeConfig.getInstance(project).invoke(abiFile.hash, fn, preExec, !preExec);
          handler.notifyTextAvailableWithTimestamp("Response: " + JSON.toJSONString(resp, true), ProcessOutputTypes.SYSTEM);
          handler.notifyProcessTerminated(0);
          notifier.notifySuccess("Ontology", "Contract invoked successfully.");
        } catch (Exception e) {
          notifier.notifyError("Ontology", e);
          handler.notifyProcessTerminated(1);
        }

        indicator.setFraction(1);
      }
    });
  }

  private void doDebug(Project project, String src, String method, JSONObject params) {
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
            public XDebugProcess start(@NotNull XDebugSession session) {
              return new OntDebugProcess(session, src, method, params);
            }
          });
    } catch (Exception e1) {
      notifier.notifyError("Ontology", e1);
    }
  }

  @Override
  protected void destroyProcessImpl() {

  }

  @Override
  protected void detachProcessImpl() {

  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }
}
