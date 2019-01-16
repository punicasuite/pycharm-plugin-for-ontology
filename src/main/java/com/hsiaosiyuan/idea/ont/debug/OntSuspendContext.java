package com.hsiaosiyuan.idea.ont.debug;


import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OntSuspendContext extends XSuspendContext {
  private XSourcePosition mySourcePosition;
  private XExecutionStack myExecutionStack;
  private OntDebugAgent myAgent;

  public OntSuspendContext(XSourcePosition sourcePosition, OntDebugAgent agent) {
    mySourcePosition = sourcePosition;
    myAgent = agent;
    myExecutionStack = initExecutionStack();
  }

  private OntExecutionStack initExecutionStack() {
    OntStackFrame frame = new OntStackFrame(mySourcePosition, myAgent);

    return new OntExecutionStack("Main Routine", frame);
  }

  @Nullable
  @Override
  public XExecutionStack getActiveExecutionStack() {
    return myExecutionStack;
  }

  @NotNull
  @Override
  public XExecutionStack[] getExecutionStacks() {
    return new XExecutionStack[]{myExecutionStack};
  }
}
