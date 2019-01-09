package com.hsiaosiyuan.idea.ont.module;

import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class OntDetectedSourceRoot extends DetectedSourceRoot {

  public OntDetectedSourceRoot(File directory) {
    super(directory, "");
  }

  @NotNull
  @Override
  public String getRootTypeName() {
    return "Ontology";
  }
}
