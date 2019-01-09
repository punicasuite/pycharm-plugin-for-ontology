package com.hsiaosiyuan.idea.ont.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

public class OntRunCmdState extends CommandLineState {
  private final RunConfiguration runCfg;

  public OntRunCmdState(RunConfiguration runConfiguration, ExecutionEnvironment env) {
    super(env);
    runCfg = runConfiguration;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    GeneralCommandLine commandLine = makeCommandLine();

    OSProcessHandler osProcessHandler;
    osProcessHandler = new OntRunCmdHandler(commandLine.createProcess(), commandLine.getCommandLineString());

    ProcessTerminatedListener.attach(osProcessHandler, runCfg.getProject());

    return osProcessHandler;
  }

  protected GeneralCommandLine makeCommandLine() {
    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath("which");
    commandLine.addParameter("punica-ts");
    commandLine.setWorkDirectory("/Users/hsy/ws/ontdev");
    return commandLine;
  }
}
