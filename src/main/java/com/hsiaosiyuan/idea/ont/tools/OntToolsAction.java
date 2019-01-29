package com.hsiaosiyuan.idea.ont.tools;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OntToolsAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    e.getPresentation().setIcon(OntIcons.ICON);
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    OntToolsDialogWrap dialog = new OntToolsDialogWrap();
    dialog.showNonblock();
  }
}
