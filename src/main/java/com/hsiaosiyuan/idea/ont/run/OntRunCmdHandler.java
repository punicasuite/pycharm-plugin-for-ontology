package com.hsiaosiyuan.idea.ont.run;

import com.intellij.execution.process.ColoredProcessHandler;
import org.jetbrains.annotations.NotNull;

public class OntRunCmdHandler extends ColoredProcessHandler {
  public OntRunCmdHandler(@NotNull Process process, String commandLine) {
    super(process, commandLine);
  }

  @Override
  protected boolean shouldDestroyProcessRecursively() {
    return true;
  }

}
