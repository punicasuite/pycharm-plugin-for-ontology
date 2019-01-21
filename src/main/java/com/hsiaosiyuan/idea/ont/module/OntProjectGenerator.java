package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class OntProjectGenerator extends WebProjectTemplate {
  @Override
  public String getDescription() {
    return "Add support for Ontology Smart Contract Development";
  }

  @Override
  public Icon getIcon() {
    return OntIcons.ICON;
  }

  @NotNull
  @Override
  public String getName() {
    return "Ontology";
  }

  @Override
  public void generateProject(
      @NotNull Project project,
      @NotNull VirtualFile baseDir,
      @NotNull Object settings,
      @NotNull Module module) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      final ModifiableRootModel modifiableModel = ModifiableModelsProvider.SERVICE.getInstance().getModuleModifiableModel(module);
      OntModuleType.getInstance().createModuleBuilder().moduleCreated(modifiableModel.getModule());
      ModifiableModelsProvider.SERVICE.getInstance().commitModuleModifiableModel(modifiableModel);
    });
  }

  @NotNull
  @Override
  public ProjectGeneratorPeer createPeer() {
    return new OntGeneratorPeer();
  }
}
