package com.hsiaosiyuan.idea.ont.debug;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OntDebuggerEditorsProvider extends XDebuggerEditorsProviderBase {
  @Override
  protected PsiFile createExpressionCodeFragment(@NotNull Project project, @NotNull String text, @Nullable PsiElement context, boolean isPhysical) {
    return context.getContainingFile();
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return PythonFileType.INSTANCE;
  }
}
