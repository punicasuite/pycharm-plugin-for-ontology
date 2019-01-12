package com.hsiaosiyuan.idea.ont.punica.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;

public class OntNetworkConfig {
  public static final String FILENAME = "punica-config.json";
  private static OntNetworkConfig inst = null;

  public String defaultNet;
  public HashMap<String, Network> networks;

  private Project project;

  public static OntNetworkConfig getInstance(Project project) throws IOException {
    if (inst == null) {
      inst = new OntNetworkConfig();
    }

    inst.project = project;
    inst.load();
    return inst;
  }

  public static class Network {
    public String host;
    public int port;
  }

  @JSONField(serialize = false)
  public Path getFilePath() {
    return Paths.get(Objects.requireNonNull(project.getBasePath()))
        .resolve("./" + FILENAME).normalize();
  }

  @JSONField(serialize = false)
  public byte[] getRaw() throws IOException {
    Path file = getFilePath();
    return Files.readAllBytes(file);
  }

  public void load() throws IOException {
    OntNetworkConfig obj = JSON.parseObject(new String(getRaw()), OntNetworkConfig.class);
    defaultNet = obj.defaultNet;
    networks = obj.networks;
  }

  public void save() throws IOException {
    String raw = JSON.toJSONString(this, true);
    Files.write(getFilePath(), raw.getBytes());
  }

  @Nullable
  @JSONField(serialize = false)
  public String getRpcAddr() {
    Network network = networks.get(defaultNet);
    if (network == null) return null;

    return network.host + ":" + network.port;
  }
}
