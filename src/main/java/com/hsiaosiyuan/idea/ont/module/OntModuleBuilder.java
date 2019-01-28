package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.punica.OntInitProcessHandler;
import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.sdk.OntSdkSettingsStep;
import com.hsiaosiyuan.idea.ont.sdk.OntSdkType;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

public class OntModuleBuilder extends ModuleBuilder implements ModuleBuilderListener {
  public OntModuleBuilder() {
    addListener(this);
  }

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) {
    // Make the entire module directory a source root.
    ContentEntry contentEntry = doAddContentEntry(modifiableRootModel);
    if (contentEntry != null) {
      final VirtualFile file = contentEntry.getFile();
      if (file != null && file.isDirectory())
        contentEntry.addSourceFolder(file, false);
    }
  }

  @Override
  public ModuleType getModuleType() {
    return OntModuleType.getInstance();
  }

  @Override
  public void moduleCreated(@NotNull Module module) {
    OntInitProcessHandler initProcess = new OntInitProcessHandler();

    initProcess.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        super.processTerminated(event);
        ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            setupModule(module);
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
          } catch (ConfigurationException e) {
            e.printStackTrace();
          }
        });
      }
    });

    OntPunica.startProcess(initProcess, module.getProject());
    initProcess.start(module.getProject());
  }

  @Override
  public ModuleWizardStep modifyProjectTypeStep(@NotNull SettingsStep settingsStep) {
    return new OntSdkSettingsStep(settingsStep, this, id -> OntSdkType.getInstance() == id);
  }
}
