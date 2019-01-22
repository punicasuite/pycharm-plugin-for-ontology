package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OntProjectSettings implements ActionListener {
  private JTextField txPath;
  private JButton btnChoose;
  private JPanel myPanel;

  private String prevPath = "";
  private PathChangedListener pathChangedListener;

  public OntProjectSettings() {

    Logger.getInstance("ont").info("Suggest: " + OntPunica.getSuggestPath());
    txPath.setText(OntPunica.getSuggestPath());

    txPath.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        triggerPathChanged();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        triggerPathChanged();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        triggerPathChanged();
      }
    });

    btnChoose.addActionListener(this);
  }

  public String getPath() {
    return txPath.getText().trim();
  }

  public JPanel getContainer() {
    return myPanel;
  }

  public void setPathChangedListener(PathChangedListener pathChangedListener) {
    this.pathChangedListener = pathChangedListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory
        .createSingleFileNoJarsDescriptor()
        .withTitle("Choose Ontdev CLI");

    FileChooser.chooseFile(descriptor, null, null, file -> {
      txPath.setText(file.getPath());
    });
  }

  private void triggerPathChanged() {
    String curPath = txPath.getText().trim();
    if (!prevPath.equals(curPath) && pathChangedListener != null) {
      pathChangedListener.run(txPath.getText().trim());
      prevPath = curPath;
    }
  }

  public interface PathChangedListener {
    void run(String path);
  }
}
