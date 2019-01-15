package com.hsiaosiyuan.idea.ont.debug;

import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OntStackFrame extends XStackFrame {
  private XSourcePosition mySourcePosition;

  public OntStackFrame(@NotNull XSourcePosition sourcePosition) {
    mySourcePosition = sourcePosition;
  }

  @Nullable
  @Override
  public XSourcePosition getSourcePosition() {
    return mySourcePosition;
  }
}
