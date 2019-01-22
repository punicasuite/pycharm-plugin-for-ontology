package com.hsiaosiyuan.idea.ont.sdk;

import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class OntSdkSettingsStep extends ModuleWizardStep implements ActionListener {
  private final WizardContext myWizardContext;
  private final ProjectSdksModel myModel;
  private final ModuleBuilder myModuleBuilder;

  private JTextField txPath;
  private JButton btnChoose;
  private JPanel panel;

  public OntSdkSettingsStep(SettingsStep settingsStep, @NotNull ModuleBuilder moduleBuilder,
                            @NotNull Condition<? super SdkTypeId> sdkTypeIdFilter) {
    this(settingsStep, moduleBuilder, sdkTypeIdFilter, null);
  }

  public OntSdkSettingsStep(SettingsStep settingsStep,
                            @NotNull ModuleBuilder moduleBuilder,
                            @NotNull Condition<? super SdkTypeId> sdkTypeIdFilter,
                            @Nullable Condition<? super Sdk> sdkFilter) {
    this(settingsStep.getContext(), moduleBuilder, sdkTypeIdFilter, sdkFilter);
    settingsStep.addSettingsField("Ontdev:", panel);
  }

  public OntSdkSettingsStep(WizardContext context, @NotNull ModuleBuilder moduleBuilder, @NotNull Condition<? super SdkTypeId> sdkTypeIdFilter, @Nullable Condition<? super Sdk> sdkFilter) {

    myModuleBuilder = moduleBuilder;

    myWizardContext = context;
    myModel = new ProjectSdksModel();
    Project project = myWizardContext.getProject();
    myModel.reset(project);

    txPath.setText(OntSdkSettings.getInstance().PUNICA_BIN);
    if (txPath.getText().equals("")) {
      txPath.setText(OntPunica.getSuggestPath());
    }

    btnChoose.addActionListener(this);
  }

  @Override
  public JComponent getComponent() {
    return panel;
  }

  @Override
  public void updateDataModel() {

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory
        .createSingleFileNoJarsDescriptor()
        .withTitle("Choose Ontdev CLI");

    FileChooser.chooseFile(descriptor, myWizardContext.getProject(), null, file -> {
      txPath.setText(file.getPath());
    });
  }

  @Override
  public boolean validate() throws ConfigurationException {
    String path = txPath.getText();
    if (path == null || path.isEmpty()) {
      throw new ConfigurationException("Please specify the path of Ontdev CLI :(");
    }

    AtomicBoolean ok = new AtomicBoolean(false);
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      ok.set(new OntPunica(path).exist());
    }, "Checking...", false, myWizardContext.getProject());

    if (!ok.get()) throw new ConfigurationException("Unable to detect Ontdev CLI via the given path :(");
    OntSdkSettings.getInstance().PUNICA_BIN = path;
    return true;
  }
}
