package com.hsiaosiyuan.idea.ont.punica.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;

public class OntPasswordDialog extends DialogWrapper {
  private JPanel myPanel;
  private JPasswordField myPwd;
  private JCheckBox showHideCheckBox;
  private JLabel myAccLabel;

  protected OntPasswordDialog(@Nullable Project project, String acc) {
    super(project);
    init();

    myAccLabel.setText("Using account: " + acc);

    showHideCheckBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        myPwd.setEchoChar((char) 0);
      } else {
        myPwd.setEchoChar('*');
      }
    });
  }

  public String getPassword() {
    return String.valueOf(myPwd.getPassword());
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }
}
