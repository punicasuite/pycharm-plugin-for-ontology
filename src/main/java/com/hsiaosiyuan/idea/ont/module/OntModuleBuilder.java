package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.punica.OntPunica;
import com.hsiaosiyuan.idea.ont.punica.OntPunicaFactory;
import com.hsiaosiyuan.idea.ont.sdk.OntSdkSettingsStep;
import com.hsiaosiyuan.idea.ont.sdk.OntSdkType;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.concurrency.Semaphore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class OntModuleBuilder extends ModuleBuilder implements ModuleBuilderListener {
  private static boolean isBuilding = false;

  public OntModuleBuilder() {
    addListener(this);
  }

  public static boolean getIsBuilding() {
    return isBuilding;
  }

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
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
    isBuilding = true;

    Project project = module.getProject();
    OntNotifier notifier = OntNotifier.getInstance(project);

    Path tmpWorkDir;
    try {
      tmpWorkDir = Files.createTempDirectory("ont");
    } catch (IOException e) {
      notifier.notifyError("Punica Error", e);
      return;
    }

    GeneralCommandLine initCmd = OntPunicaFactory.create().makeInitCmd(tmpWorkDir.toString());

    Semaphore sm = new Semaphore();
    sm.down();
    copyTemp2Project(sm, tmpWorkDir, module);

    OntPunica.startCmdProcess(initCmd, project, evt -> sm.up());
  }

  private void copyTemp2Project(Semaphore sm, Path tmpWorkDir, Module module) {
    Project project = module.getProject();

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        sm.waitFor();

        OntNotifier notifier = OntNotifier.getInstance(project);
        File src = tmpWorkDir.toFile();
        File dest = new File(Objects.requireNonNull(project.getBasePath()));

        IOFileFilter gitFilter = FileFilterUtils.nameFileFilter(".git");
        FileFilter filter = FileFilterUtils.notFileFilter(gitFilter);

        try {
          FileUtils.copyDirectory(src, dest, filter);
        } catch (IOException e) {
          notifier.notifyError("Punica Error", e);
        }

        notifyFinished();
      }

      @Override
      public void onSuccess() {
        ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            isBuilding = false;
            setupModule(module);
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
          } catch (ConfigurationException e) {
            e.printStackTrace();
          }
        });
      }
    });
  }


  @Override
  public ModuleWizardStep modifyProjectTypeStep(@NotNull SettingsStep settingsStep) {
    return new OntSdkSettingsStep(settingsStep, this, id -> OntSdkType.getInstance() == id);
  }
}
