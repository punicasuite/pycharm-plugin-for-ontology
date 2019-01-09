package com.hsiaosiyuan.idea.ont.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class OntRunConfigurationProducer extends RunConfigurationProducer<OntRunConfiguration> {

  public OntRunConfigurationProducer() {
    super(new OntConfigurationType());
  }

  protected OntRunConfigurationProducer(@NotNull ConfigurationType configurationType) {
    super(configurationType);
  }

  @Override
  protected boolean setupConfigurationFromContext(OntRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
    PsiElement elem = context.getPsiLocation();
    PsiFile file = elem != null ? elem.getContainingFile() : null;
    return file != null;
  }

  @Override
  public boolean isConfigurationFromContext(OntRunConfiguration configuration, ConfigurationContext context) {
    return false;
  }
}
