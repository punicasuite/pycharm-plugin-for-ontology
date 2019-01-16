package com.hsiaosiyuan.idea.ont.sdk;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "OntSdkSettings", storages = @Storage("ont-sdk.xml"))
public class OntSdkSettings implements PersistentStateComponent<OntSdkSettings> {
  public static OntSdkSettings getInstance() {
    return ServiceManager.getService(OntSdkSettings.class);
  }

  public String PUNICA_BIN;

  @Nullable
  @Override
  public OntSdkSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull OntSdkSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
