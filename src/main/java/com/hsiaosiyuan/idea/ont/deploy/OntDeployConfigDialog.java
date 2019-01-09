package com.hsiaosiyuan.idea.ont.deploy;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OntDeployConfigDialog extends DialogWrapper {

  private JPanel panel;
  private JTextField textField1;
  private JTextField textField2;
  private JTextField textField3;
  private JTextField textField4;
  private JTextField textField5;

  public OntDeployConfigDialog(@Nullable Project project) {
    super(project);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }
}
