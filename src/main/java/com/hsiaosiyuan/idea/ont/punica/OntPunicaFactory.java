package com.hsiaosiyuan.idea.ont.punica;

import com.hsiaosiyuan.idea.ont.sdk.OntSdkSettings;

public class OntPunicaFactory {

  public static OntPunica create(String binPath) {
    return new OntPunica(binPath);
  }

  public static OntPunica create() {
    return create(OntSdkSettings.getInstance().PUNICA_BIN);
  }
}
