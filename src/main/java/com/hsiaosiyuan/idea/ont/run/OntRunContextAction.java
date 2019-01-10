package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.deploy.OntDeployConfigDialog;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.ui.OntNotifier;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.actions.RunContextAction;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public class OntRunContextAction extends RunContextAction {
  private final Executor myExecutor;

  public OntRunContextAction(@NotNull Executor executor) {
    super(executor);
    myExecutor = executor;
  }

  @Override
  protected void perform(final ConfigurationContext context) {
    final RunManagerEx runManager = (RunManagerEx) context.getRunManager();
    RunnerAndConfigurationSettings configuration = runManager.getSelectedConfiguration();

    // prepare configs
    if (!(configuration instanceof OntConfigurationType)) {
      Optional<RunnerAndConfigurationSettings> st = runManager.getAllSettings()
          .stream().filter((s) -> s.getType() instanceof OntConfigurationType).findFirst();
      configuration = st.orElse(null);
    }

    if (configuration != null) {
      runManager.setSelectedConfiguration(configuration);
    } else {
      ConfigurationFromContext ctx = RunConfigurationProducer.getInstance(OntRunConfigurationProducer.class)
          .createConfigurationFromContext(context);

      assert ctx != null;

      configuration = ctx.getConfigurationSettings();
      configuration.setName("Ontology");
      runManager.addConfiguration(configuration);
      runManager.setSelectedConfiguration(configuration);
    }

    // check whether the contract was deployed
    PsiElement element = context.getPsiLocation();
    if (element == null) return;

    String srcAbs = element.getContainingFile().getVirtualFile().getPath();

    OntRunConfigurationOptions cfg = OntRunConfigurationOptions.getInstance(context.getProject());
    assert cfg != null;

    OntNotifier notifier = OntNotifier.getInstance(context.getProject());
    String rpcAddr = cfg.getRpcAddr();
    if (rpcAddr == null) {
      notifier.notifyError("Ontology Error", "Unable to retrieve RPC address");
      return;
    }

    AbiIndexManager abiIndexManager = AbiIndexManager.getInstance();
    AbiFile abiFile = abiIndexManager.src2abi.get(srcAbs);
    assert abiFile != null;

    boolean isDeployed;
    try {
      isDeployed = OntPunica.isContractDeployed(rpcAddr, abiFile.hash);
    } catch (Exception e) {
      notifier.notifyError("Ontology Error", "Unable to check contract state: " + e.getMessage());
      return;
    }

    if (!isDeployed) {
      OntDeployConfigDialog dialog;
      try {
        dialog = new OntDeployConfigDialog(context.getProject());
      } catch (IOException e) {
        notifier.notifyError("Ontology", "Unable to load config: " + e.getMessage());
        return;
      }
      if (!dialog.showAndGet()) return;
    }

    ExecutionUtil.doRunConfiguration(configuration, myExecutor, null, null, context.getDataContext());
  }

}
