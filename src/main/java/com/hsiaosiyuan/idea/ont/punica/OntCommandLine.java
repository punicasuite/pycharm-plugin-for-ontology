package com.hsiaosiyuan.idea.ont.punica;

import com.intellij.execution.configurations.GeneralCommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OntCommandLine extends GeneralCommandLine {

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

}
