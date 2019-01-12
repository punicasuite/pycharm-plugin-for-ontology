package com.hsiaosiyuan.idea.ont.run;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class OntProcessHandler extends ProcessHandler {

  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public SimpleDateFormat getFormatter() {
    return FORMATTER;
  }

  public void notifyTextAvailableWithTimestamp(@NotNull String text, @NotNull Key outputType) {
    text = getFormatter().format(new Date()) + ": " + text + "\n";
    notifyTextAvailable(text, outputType);
  }
}
