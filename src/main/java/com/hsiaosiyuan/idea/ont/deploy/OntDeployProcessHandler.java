package com.hsiaosiyuan.idea.ont.deploy;

import com.alibaba.fastjson.JSON;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OntDeployProcessHandler extends OntProcessHandler {
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

  public void start(final Project project, final String avmPath) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    String src = AbiFile.avmPath2SrcPath(avmPath);
    String filename = AbiFile.extractSrcFilename(src);

    notifyTextAvailableWithTimestamp("Deploying contract: " + filename, ProcessOutputTypes.SYSTEM);
    notifyTextAvailableWithTimestamp("Waiting for user's interaction...", ProcessOutputTypes.SYSTEM);

    OntDeployConfigDialog dialog;
    try {
      dialog = new OntDeployConfigDialog(project, filename);
    } catch (IOException e) {
      notifyProcessTerminated(1);
      notifier.notifyError("Ontology", "Unable to load config: " + e.getMessage());
      return;
    }

    if (!dialog.showAndGet()) {
      notifyProcessTerminated(1);
      notifyTextAvailableWithTimestamp("User canceled", ProcessOutputTypes.STDERR);
      return;
    }

    final OntDeployProcessHandler handler = this;
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Deploy") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          indicator.setIndeterminate(true);

          OntNetworkConfig networkConfig = OntNetworkConfig.getInstance(project);
          handler.notifyTextAvailableWithTimestamp("Sending to " + networkConfig.getRpcAddr(), ProcessOutputTypes.SYSTEM);

          String code = new String(Files.readAllBytes(Paths.get(AbiFile.srcPath2AvmPath(src)))).trim();
          Object resp = OntDeployConfig.getInstance(project).deploy(code);
          handler.notifyTextAvailableWithTimestamp("Response: " + JSON.toJSONString(resp, true), ProcessOutputTypes.SYSTEM);
          handler.notifyProcessTerminated(0);
          notifier.notifySuccess("Ontology", "Contract deployed successfully.");
        } catch (Exception e) {
          notifier.notifyError("Ontology", "Failed to deploy contract: " + e.getMessage());
          handler.notifyProcessTerminated(1);
        }

        indicator.setFraction(1);
      }
    });
  }
}
