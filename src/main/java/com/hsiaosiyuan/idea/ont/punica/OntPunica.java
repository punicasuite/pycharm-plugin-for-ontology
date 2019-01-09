package com.hsiaosiyuan.idea.ont.punica;

import com.github.ontio.OntSdk;
import com.github.ontio.network.exception.ConnectorException;
import com.github.ontio.network.exception.RpcException;
import com.github.ontio.sdk.exception.SDKException;
import com.hsiaosiyuan.idea.ont.run.OntRunCmdHandler;
import com.hsiaosiyuan.idea.ont.ui.OntConsoleToolWindowFactory;
import com.hsiaosiyuan.idea.ont.ui.OntNotifier;
import com.hsiaosiyuan.idea.ont.util.OntSystemUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OntPunica {
  private File bin;
  private String version;

  public OntPunica(String binPath) {
    bin = new File(binPath);
  }

  public boolean exist() {
    if (!bin.isFile() || !bin.canExecute()) return false;

    ProcessOutput out = exec();
    if (out == null) return false;

    return out.getStdout().contains("Punica CLI");
  }

  @Nullable
  public ProcessOutput exec(final boolean desireOk, final String... arguments) {
    try {
      ProcessOutput out = OntSystemUtil.getProcessOutput(bin.getAbsolutePath(), arguments);
      if (desireOk && out.getExitCode() != 0) return null;
      return out;
    } catch (ExecutionException e) {
      return null;
    }
  }

  @Nullable
  public ProcessOutput exec(final String... arguments) {
    return exec(true, arguments);
  }

  @Nullable
  public String getVersion() {
    if (version != null) return version;

    if (!exist()) return null;

    ProcessOutput out = exec("-v");
    if (out == null) return null;

    version = out.getStdout().trim();
    return version;
  }

  public GeneralCommandLine makeInitCmd(String wd) {
    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(bin.getAbsolutePath());
    commandLine.addParameter("init");
    commandLine.addParameter("-p");
    commandLine.addParameters(wd);
    return commandLine;
  }

  public GeneralCommandLine makeCompileCmd(String wd, String contractsDir) {

    if (contractsDir.startsWith("/")) {
      contractsDir = new File(wd).toURI().relativize(new File(contractsDir).toURI()).getPath();
    }

    GeneralCommandLine commandLine = new GeneralCommandLine() {
      @NotNull
      @Override
      protected Process startProcess(@NotNull List<String> escapedCommands) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(escapedCommands);
        Map<String, String> env = builder.environment();
        setupEnvironment(env);
        env.put("NODE_NO_WARNINGS", "1");
        builder.directory(getWorkDirectory());
        builder.redirectErrorStream(isRedirectErrorStream());
        return builder.start();
      }
    };

    commandLine.setExePath(bin.getAbsolutePath());
    commandLine.addParameter("compile");
    commandLine.addParameter("-p");
    commandLine.addParameters(wd);
    commandLine.addParameters("--contracts");
    commandLine.addParameters(contractsDir);
    return commandLine;
  }

  public static void startCmProcess(GeneralCommandLine cmd, Project project, @Nullable Consumer<ProcessEvent> onTerminated) {
    OntNotifier notifier = OntNotifier.getInstance(project);
    OSProcessHandler osProcessHandler;
    try {
      osProcessHandler = new OntRunCmdHandler(cmd.createProcess(), cmd.getCommandLineString());
    } catch (ExecutionException err) {
      notifier.notifyError("Punica Error", err.getMessage());
      return;
    }

    if (onTerminated != null) {
      osProcessHandler.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          onTerminated.consume(event);
        }
      });
    }

    ProcessTerminatedListener.attach(osProcessHandler, project);
    ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

    ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(OntConsoleToolWindowFactory.ID);
    ContentImpl content = new ContentImpl(consoleView.getComponent(), "Punica", true);
    window.getContentManager().addContent(content);
    window.getContentManager().setSelectedContent(content, true);

    window.activate(null);
    consoleView.attachToProcess(osProcessHandler);

    osProcessHandler.startNotify();
  }

  public static boolean isContractDeployed(String rpcAddress, String codeHash) throws SDKException, ConnectorException, IOException {
    OntSdk sdk = OntSdk.getInstance();
    sdk.setRpc(rpcAddress);
    try {
      sdk.getRpc().getContract(codeHash);
    } catch (RpcException e) {
      if (e.getMessage().contains("unknow contract") || e.getMessage().contains("unknow contracts")) {
        return false;
      }
      throw e;
    }
    return true;
  }
}
