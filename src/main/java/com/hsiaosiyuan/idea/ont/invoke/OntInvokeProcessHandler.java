package com.hsiaosiyuan.idea.ont.invoke;

import com.alibaba.fastjson.JSON;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.hsiaosiyuan.idea.ont.run.OntProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class OntInvokeProcessHandler extends OntProcessHandler {
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

    OntInvokeDialog invokeDialog;
    try {
      invokeDialog = new OntInvokeDialog(project, src, method);
    } catch (Exception e) {
      notifyProcessTerminated(1);
      notifier.notifyError("Ontology", e);
      return;
    }

    if (!invokeDialog.showAndGet()) {
      notifyProcessTerminated(1);
      notifyTextAvailableWithTimestamp("User canceled", ProcessOutputTypes.STDERR);
      return;
    }

    AbiFunction fn;
    try {
      fn = invokeDialog.getFn();
    } catch (Exception e) {
      notifyProcessTerminated(1);
      notifier.notifyError("Ontology", e);
      return;
    }


    final OntInvokeProcessHandler handler = this;
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Deploy") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);

        AbiFile abiFile = AbiIndexManager.getInstance().getAbi(src);
        try {
          OntNetworkConfig networkConfig = OntNetworkConfig.getInstance(project);
          handler.notifyTextAvailableWithTimestamp("Sending to " + networkConfig.getRpcAddr(), ProcessOutputTypes.SYSTEM);

          Object resp = OntDeployConfig.getInstance(project).invoke(abiFile.hash, fn, invokeDialog.getPreExec(), invokeDialog.getWaitResult());
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
}
