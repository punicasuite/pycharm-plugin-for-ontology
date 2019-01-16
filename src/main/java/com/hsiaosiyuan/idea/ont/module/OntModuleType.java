package com.hsiaosiyuan.idea.ont.module;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class OntModuleType extends ModuleType<OntModuleBuilder> {

  public static final String ID = "ONTOLOGY_MODULE";

  public static OntModuleType getInstance() {
    return (OntModuleType) ModuleTypeManager.getInstance().findByID(ID);
  }

  public OntModuleType() {
    super(ID);
  }

  @NotNull
  @Override
  public OntModuleBuilder createModuleBuilder() {
    return new OntModuleBuilder();
  }

  @NotNull
  @Override
  public String getName() {
    return "Smart Contract";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Add support for Ontology Smart Contract Development";
  }

  @Override
  public Icon getNodeIcon(boolean isOpened) {
    return OntIcons.ICON;
  }


}
