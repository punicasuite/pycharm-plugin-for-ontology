package com.hsiaosiyuan.idea.ont.run;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.hsiaosiyuan.idea.ont.module.OntProjectStructureDetector;
import com.hsiaosiyuan.idea.ont.run.option.Network;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;

public class OntRunConfigurationOptions {
  public HashMap<String, Network> networks = new HashMap<>();

  public String defaultNetwork;

  private Project project;

  private static OntRunConfigurationOptions instance = null;

  @Nullable
  public static OntRunConfigurationOptions getInstance(Project project) {
    if (instance != null) return instance;

    try {
      byte[] data = loadConfig(project);
      instance = JSON.parseObject(data, OntRunConfigurationOptions.class);
      instance.project = project;
      return instance;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] loadConfig(Project project) throws IOException {
    Path file = Paths.get(Objects.requireNonNull(project.getBasePath()))
        .resolve(OntProjectStructureDetector.CFG_FILENAME);
    return Files.readAllBytes(file);
  }

  public void save() {
    if (project.isDisposed()) return;

    String json = JSON.toJSONString(this, true);
    Path file = Paths.get(Objects.requireNonNull(project.getBasePath()))
        .resolve(OntProjectStructureDetector.CFG_FILENAME);
    try {
      Files.write(file, json.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Nullable
  @JSONField(serialize = false)
  public String getRpcAddr() {
    Network network = networks.get(defaultNetwork);
    if (network == null) return null;

    return network.host + ":" + network.port;
  }
}
