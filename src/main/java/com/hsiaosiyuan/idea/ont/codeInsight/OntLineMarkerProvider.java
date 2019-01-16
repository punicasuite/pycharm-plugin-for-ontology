package com.hsiaosiyuan.idea.ont.codeInsight;

import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.debug.OntDebugAction;
import com.hsiaosiyuan.idea.ont.run.OntRunAction;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.jetbrains.python.psi.PyElementType;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class OntLineMarkerProvider implements LineMarkerProvider {

  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
    return null;
  }

  @Override
  public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    if (elements.isEmpty()) return;

    elements.stream().filter((element) -> {

      PsiFile psiFile = element.getContainingFile();
      if (psiFile == null) return false;

      String srcAbs = psiFile.getVirtualFile().getPath();
      AbiIndexManager abiIndexManager = AbiIndexManager.getInstance();

      abiIndexManager.indexFromSrcFile(srcAbs, true);

      if (!abiIndexManager.hasSrcIndex(srcAbs)) return false;

      if (!(element instanceof LeafElement)) return false;

      LeafElement el = (LeafElement) element;
      if (!(el.getElementType() instanceof PyElementType)) return false;

      PyElementType type = (PyElementType) el.getElementType();
      if (!type.toString().equals("Py:IDENTIFIER")) return false;

      if (!(element.getParent() instanceof PyFunction)) return false;

      return abiIndexManager.hasFn(srcAbs, element.getText());

    }).forEach((element) -> {

      String src = element.getContainingFile().getVirtualFile().getPath();
      String method = element.getText();
      final AnAction runAction = new OntRunAction(src, method);
      final AnAction debugAction = new OntDebugAction(src, method);

      final DefaultActionGroup group = new DefaultActionGroup();
      group.add(debugAction);

      LineMarkerInfo<PsiElement> info = new LineMarkerInfo<PsiElement>(
          element,
          element.getTextRange(),
          AllIcons.Actions.Execute,
          Pass.LINE_MARKERS,
          e -> "Invoke '" + e.getContainingFile().getVirtualFile().getNameWithoutExtension() + "::" + e.getText() + "'",
          null,
          GutterIconRenderer.Alignment.RIGHT) {

        @NotNull
        @Override
        public GutterIconRenderer createGutterRenderer() {
          return new LineMarkerGutterIconRenderer<PsiElement>(this) {
            @Override
            public AnAction getClickAction() {
              return runAction;
            }

            @NotNull
            @Override
            public ActionGroup getPopupMenuActions() {
              return group;
            }
          };
        }

      };

      result.add(info);

    });
  }
}
