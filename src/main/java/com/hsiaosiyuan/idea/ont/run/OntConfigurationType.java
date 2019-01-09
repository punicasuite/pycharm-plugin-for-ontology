package com.hsiaosiyuan.idea.ont.run;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class OntConfigurationType extends ConfigurationTypeBase {

  public OntConfigurationType() {
    super("OdRunType", "Run Smart Contract", "Run Smart Contract of Ontology", OntIcons.ICON);

    addFactory(new ConfigurationFactory(this) {
      @NotNull
      @Override
      public RunConfigurationSingletonPolicy getSingletonPolicy() {
        return RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY;
      }

      @NotNull
      @Override
      public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new OntRunConfiguration(project, this, " Template config");
      }
    });

  }
}
