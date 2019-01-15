package com.hsiaosiyuan.idea.ont.debug;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.jetbrains.python.debugger.PyLineBreakpointType;
import org.jetbrains.annotations.NotNull;

public class OntLineBreakpointHandler extends XBreakpointHandler {
  private OntDebugAgent agent;

  protected OntLineBreakpointHandler(OntDebugAgent agent) {
    super(PyLineBreakpointType.class);
    this.agent = agent;
  }

  @Override
  public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
    agent.addBreakpoint(breakpoint);
  }

  @Override
  public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {
    agent.removeBreakpoint(breakpoint);
  }
}
