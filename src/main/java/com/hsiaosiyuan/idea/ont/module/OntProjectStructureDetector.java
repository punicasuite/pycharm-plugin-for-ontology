package com.hsiaosiyuan.idea.ont.module;

import com.alibaba.fastjson.JSON;
import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.importProject.ProjectDescriptor;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OntProjectStructureDetector extends ProjectStructureDetector {

  public static final String CFG_FILENAME = "punica-config.json";

  @NotNull
  @Override
  public DirectoryProcessingResult detectRoots(
      @NotNull File dir,
      @NotNull File[] children,
      @NotNull File base,
      @NotNull List<DetectedProjectRoot> result) {

    for (File child : children) {
      if (isConfigFile(child)) {
        result.add(new OntDetectedSourceRoot(child.getParentFile()));
      }
    }
    return DirectoryProcessingResult.SKIP_CHILDREN;
  }

  @Override
  public void setupProjectStructure(@NotNull Collection<DetectedProjectRoot> roots,
                                    @NotNull ProjectDescriptor projectDescriptor,
                                    @NotNull final ProjectFromSourcesBuilder builder) {
    if (!roots.isEmpty() && !builder.hasRootsFromOtherDetectors(this)) {
      List<ModuleDescriptor> modules = projectDescriptor.getModules();
      if (modules.isEmpty()) {
        modules = new ArrayList<>();
        for (DetectedProjectRoot root : roots) {
          modules.add(new ModuleDescriptor(new File(builder.getBaseProjectPath()), OntModuleType.getInstance(), (DetectedSourceRoot) root) {

            @Override
            public void updateModuleConfiguration(Module module, ModifiableRootModel rootModel) {
              super.updateModuleConfiguration(module, rootModel);
              for (ModuleBuilder moduleBuilder : ModuleBuilder.getAllBuilders()) {
                if (moduleBuilder instanceof OntModuleBuilder) {
                  ((OntModuleBuilder) moduleBuilder).moduleCreated(module);
                  return;
                }
              }
            }
          });
        }
        projectDescriptor.setModules(modules);
      }
    }
  }

  private boolean isConfigFile(File file) {
    if (!file.getName().equals(CFG_FILENAME)) {
      return false;
    }
    try {
      byte[] data = Files.readAllBytes(file.toPath());
      JSON.parse(data);
    } catch (IOException e) {
      return false;
    }
    return true;
  }

}
