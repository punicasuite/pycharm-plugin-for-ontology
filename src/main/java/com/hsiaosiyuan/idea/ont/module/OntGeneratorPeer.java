package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.sdk.OntSdkSettings;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectGeneratorPeer;
import com.intellij.platform.WebProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class OntGeneratorPeer implements ProjectGeneratorPeer<OntProjectSettingData> {
  private OntProjectSettings settings;
  private SettingsListener listener;

  public OntGeneratorPeer() {
    settings = new OntProjectSettings();

    settings.setPathChangedListener(path -> {
      if (listener != null)
        listener.stateChanged(false);
    });
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return settings.getContainer();
  }

  @Override
  public void buildUI(@NotNull SettingsStep settingsStep) {

  }

  @NotNull
  @Override
  public OntProjectSettingData getSettings() {
    return new OntProjectSettingData(settings.getPath());
  }

  @Nullable
  @Override
  public ValidationInfo validate() {
    AtomicBoolean ok = new AtomicBoolean(false);
    String path = settings.getPath().trim();

    if (path.equals("")) return null;

    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      ok.set(new OntPunica(path).exist());
    }, "Checking...", false, null);

    if (!ok.get()) {
      return new ValidationInfo("The path of Ontdev CLI is invalid");
    }

    OntSdkSettings.getInstance().PUNICA_BIN = path;
    return null;
  }

  @Override
  public boolean isBackgroundJobRunning() {
    return false;
  }

  @Override
  public void addSettingsStateListener(@NotNull WebProjectGenerator.SettingsStateListener listener) {

  }

  @Override
  public void addSettingsListener(@NotNull SettingsListener listener) {
    this.listener = listener;
  }
}
