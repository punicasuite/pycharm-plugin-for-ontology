package com.hsiaosiyuan.idea.ont.sdk;

import com.hsiaosiyuan.idea.ont.OntIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.util.Consumer;
import com.jetbrains.python.sdk.PythonSdkDetailsStep;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class OntSdkType extends SdkType {

  public OntSdkType() {
    super("Ontdev CLI");
  }

  public static OntSdkType getInstance() {
    return SdkType.findInstance(OntSdkType.class);
  }

  @Override
  public Icon getIcon() {
    return OntIcons.ICON;
  }

  @Nullable
  @Override
  public String suggestHomePath() {
    return null;
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return false;
  }

  @NotNull
  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    return null;
  }

  @Nullable
  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel, @NotNull SdkModificator sdkModificator) {
    return null;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Ontdev CLI";
  }

  @Override
  public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {

  }

  @Override
  public boolean isRootTypeApplicable(@NotNull OrderRootType type) {
    return type == OrderRootType.CLASSES;
  }

  @Override
  public boolean supportsCustomCreateUI() {
    return true;
  }

  @Override
  public void showCustomCreateUI(@NotNull SdkModel sdkModel, @NotNull JComponent parentComponent, @Nullable Sdk selectedSdk, @NotNull Consumer<Sdk> sdkCreatedCallback) {
    Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(parentComponent));
    final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
    if (pointerInfo == null) return;

    final Point point = pointerInfo.getLocation();
    PythonSdkDetailsStep
        .show(project, null, sdkModel.getSdks(), null, parentComponent, point, null, sdk -> {
          if (sdk != null) {
            sdkCreatedCallback.consume(sdk);
          }
        });
  }
}
