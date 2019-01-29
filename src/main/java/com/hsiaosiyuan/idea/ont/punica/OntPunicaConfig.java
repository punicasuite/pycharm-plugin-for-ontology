package com.hsiaosiyuan.idea.ont.punica;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class OntPunicaConfig {
  public static final String FILENAME = "default-config.json";
  private static OntPunicaConfig inst = null;

  private Project project;

  public String defaultWallet;
  public JSONObject password;

  public static OntPunicaConfig getInstance(Project project) throws IOException {
    if (inst == null) {
      inst = new OntPunicaConfig();
    }

    inst.project = project;
    inst.load();
    return inst;
  }

  @JSONField(serialize = false)
  public Path getFilePath() {
    return Paths.get(Objects.requireNonNull(project.getBasePath()))
        .resolve("./contracts/" + FILENAME).normalize();
  }

  public byte[] getRaw() throws IOException {
    Path file = getFilePath();
    return Files.readAllBytes(file);
  }

  public void load() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));
    defaultWallet = obj.getString("defaultWallet");
    password = obj.getJSONObject("password");
  }

  public void reload(String path) throws IOException {
    path = Paths.get(path).normalize().toString();
    if (!getFilePath().toString().equals(path)) return;
    load();
  }

  public void save() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));
    obj.put("defaultWallet", defaultWallet);
    obj.put("password", password);
    String raw = JSON.toJSONString(obj, true);
    Files.write(getFilePath(), raw.getBytes());
  }

  public Path getWalletPath() {
    Path path = Paths.get(defaultWallet);
    if (path.isAbsolute()) return path;
    return Paths.get(Objects.requireNonNull(project.getBasePath())).resolve("./wallet/" + defaultWallet).normalize();
  }
}
