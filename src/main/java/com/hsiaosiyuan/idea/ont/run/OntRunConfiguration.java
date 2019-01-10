package com.hsiaosiyuan.idea.ont.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OntRunConfiguration extends LocatableConfigurationBase {

  public OntRunConfigurationOptions options;

  public OntRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String s) {
    super(project, factory, s);
    options = OntRunConfigurationOptions.getInstance(project);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new OntConfigurationEditor(getProject(), this);
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
    return new OntRunCmdState(this, environment);
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    options = OntRunConfigurationOptions.getInstance(getProject());
  }

  @Override
  public void writeExternal(@NotNull Element element) {
    super.writeExternal(element);
    options.save();
  }

  @Override
  public boolean canRunOn(@NotNull ExecutionTarget target) {
    return false;
  }
}
