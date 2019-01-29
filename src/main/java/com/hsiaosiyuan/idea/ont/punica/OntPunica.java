package com.hsiaosiyuan.idea.ont.punica;

import com.github.ontio.OntSdk;
import com.github.ontio.network.exception.ConnectorException;
import com.github.ontio.network.exception.RpcException;
import com.github.ontio.sdk.exception.SDKException;
import com.hsiaosiyuan.idea.ont.run.OntConsoleToolWindowFactory;
import com.hsiaosiyuan.idea.ont.run.OntProcessHandler;
import com.hsiaosiyuan.idea.ont.util.OntSystemUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OntPunica {
  private File bin;
  private String version;

  public OntPunica(String binPath) {
    bin = new File(binPath);
  }

  public boolean exist() {
    if (!bin.isFile() || !bin.canExecute()) return false;

    ProcessOutput out = exec("-h");
    if (out == null) return false;

    return out.getStdout().contains("ontdev");
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

  public static String getSuggestPath() {
    String cmd = "which";
    if (SystemInfo.isWindows) {
      cmd = "C:\\Windows\\System32\\where.exe";
    }
    ProcessOutput out;
    try {
      out = OntSystemUtil.getProcessOutput(cmd, "ontdev");
      if (out.getExitCode() != 0) return "";

      List<String> lines = out.getStdoutLines(true);
      if (lines.size() == 1) {
        return lines.get(0);
      }
      return lines.size() == 2 ? lines.get(1) : "";
    } catch (ExecutionException ignored) {
    }
    return "";
  }

  public GeneralCommandLine makeDebugCmd(String ticket) {
    GeneralCommandLine cmd = new OntCommandLine();
    cmd.setExePath(bin.getAbsolutePath());
    cmd.addParameters("debug");
    cmd.addParameter("--ticket");
    cmd.addParameter(ticket);
    return cmd;
  }

  public static ConsoleView makeConsoleView(Project project, String title) {
    ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

    ContentImpl content = new ContentImpl(consoleView.getComponent(), title, true);

    ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(OntConsoleToolWindowFactory.ID);
    window.getContentManager().addContent(content);
    window.getContentManager().setSelectedContent(content, true);
    window.activate(null);

    return consoleView;
  }

  public static void startProcess(OntProcessHandler processHandler, Project project) {
    ProcessTerminatedListener.attach(processHandler, project);
    ConsoleView consoleView = makeConsoleView(project, "Ontology");
    consoleView.attachToProcess(processHandler);
    processHandler.startNotify();
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
