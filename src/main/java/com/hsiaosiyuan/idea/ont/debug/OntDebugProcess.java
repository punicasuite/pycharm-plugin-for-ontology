package com.hsiaosiyuan.idea.ont.debug;

import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OntDebugProcess extends XDebugProcess {
  private OntLineBreakpointHandler lineBreakpointHandler;
  private ConsoleView consoleView;
  private OntDebugAgent agent;

  public OntDebugProcess(@NotNull XDebugSession session, @NotNull String src, @NotNull String method) {
    super(session);

    consoleView = (ConsoleView) super.createConsole();
    agent = new OntDebugAgent(session, consoleView, src, method);
    lineBreakpointHandler = new OntLineBreakpointHandler(agent);

    startAgent();
  }

  @NotNull
  @Override
  public ExecutionConsole createConsole() {
    return consoleView;
  }

  @NotNull
  @Override
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    return new XBreakpointHandler<?>[]{lineBreakpointHandler};
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new OntDebuggerEditorsProvider();
  }

  private OntNotifier getNotifier() {
    return OntNotifier.getInstance(getSession().getProject());
  }

  private void startAgent() {
    try {
      agent.start(null);
    } catch (Exception e) {
      getNotifier().notifyError("Ontology", e);
    }
  }

  @Override
  public void stop() {
    try {
      agent.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void resume(@Nullable XSuspendContext context) {
    agent.resume();
  }

  @Override
  public void startStepOver(@Nullable XSuspendContext context) {
    agent.stepOver();
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    agent.stepInto();
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    agent.stepOut();
  }
}
