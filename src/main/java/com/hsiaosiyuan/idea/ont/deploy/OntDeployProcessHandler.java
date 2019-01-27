package com.hsiaosiyuan.idea.ont.deploy;

import com.alibaba.fastjson.JSON;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
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

  @Nullable
  private String getHash(Project project, String srcPath) {
    OntNotifier notifier = OntNotifier.getInstance(project);
    AbiFile abiFile = AbiIndexManager.getInstance().getAbi(srcPath);
    if (abiFile == null) {
      notifier.notifyError("Ontology", "Unable to load abi file: " + srcPath);
      return null;
    }

    return abiFile.hash;
  }

  private int isContractAlreadyDeployed(Project project, String srcPath) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    OntNetworkConfig networkConfig;
    try {
      networkConfig = OntNetworkConfig.getInstance(project);
    } catch (IOException e) {
      notifier.notifyError("Ontology", "Unable to load network config: " + e.getMessage());
      return -1;
    }

    String hash = getHash(project, srcPath);
    if (hash == null) return -1;

    boolean isDeployed;
    String rpcAddr = networkConfig.getRpcAddr();
    try {
      isDeployed = OntPunica.isContractDeployed(rpcAddr, hash);
    } catch (Exception e) {
      notifier.notifyError("Ontology", "Unable to query contract state via rpc " + rpcAddr + ", please check you network");
      return -1;
    }
    return isDeployed ? 1 : 0;
  }

  public void start(final Project project, final String avmPath) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    String src = AbiFile.avmPath2SrcPath(avmPath);
    String filename = AbiFile.extractSrcFilename(src);

    notifyTextAvailableWithTimestamp("Deploying contract: " + filename, ProcessOutputTypes.SYSTEM);

    int state = isContractAlreadyDeployed(project, src);
    if (state == 1) {
      String hash = getHash(project, src);
      notifyTextAvailableWithTimestamp("Contract already deployed at: " + hash, ProcessOutputTypes.STDERR);
      notifyProcessTerminated(0);
      return;
    } else if (state == -1) {
      // error occurs when querying the state of contract
      notifyTextAvailableWithTimestamp("Errors occur, terminated.", ProcessOutputTypes.SYSTEM);
      notifyProcessTerminated(1);
      return;
    }

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
