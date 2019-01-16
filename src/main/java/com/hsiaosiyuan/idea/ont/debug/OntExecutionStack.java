package com.hsiaosiyuan.idea.ont.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class OntExecutionStack extends XExecutionStack {
  private OntStackFrame myTopFrame;

  protected OntExecutionStack(String displayName, OntStackFrame topFrame) {
    super(displayName);
    myTopFrame = topFrame;
  }

  @Nullable
  @Override
  public XStackFrame getTopFrame() {
    return myTopFrame;
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    container.addStackFrames(new ArrayList<XStackFrame>() {{
      add(myTopFrame);
    }}, true);
  }
}
