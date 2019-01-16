package com.hsiaosiyuan.idea.ont.debug;

import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public class OntStackFrame extends XStackFrame {
  private XSourcePosition mySourcePosition;
  private OntDebugAgent myAgent;

  public OntStackFrame(@NotNull XSourcePosition sourcePosition, @NotNull OntDebugAgent agent) {
    mySourcePosition = sourcePosition;
    myAgent = agent;
  }

  @Nullable
  @Override
  public XSourcePosition getSourcePosition() {
    return mySourcePosition;
  }

  @SuppressWarnings("Duplicates")
  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    myAgent.queryVariables(null).thenAcceptAsync((resp) -> {
      try {
        JSONArray vs = resp.getJSONArray("variables");
        final XValueChildrenList xValues = new XValueChildrenList();

        xValues.addTopGroup(new XValueGroup("Locals") {
          @Override
          public boolean isAutoExpand() {
            return true;
          }

          @Override
          public void computeChildren(@NotNull XCompositeNode node) {
            final XValueChildrenList xValues = new XValueChildrenList();

            try {
              for (int i = 0; i < vs.length(); i++) {
                JSONObject v = vs.getJSONObject(i);
                xValues.add(v.getString("name"), new OntDebugValue(v, myAgent));
              }
            } catch (Exception e) {
              e.printStackTrace();
            }

            node.addChildren(xValues, true);
            node.setAlreadySorted(false);
          }
        });

        node.addChildren(xValues, true);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
