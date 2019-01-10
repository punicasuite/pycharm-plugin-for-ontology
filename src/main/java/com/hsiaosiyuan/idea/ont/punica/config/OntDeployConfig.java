package com.hsiaosiyuan.idea.ont.punica.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.ontio.OntSdk;
import com.github.ontio.common.Helper;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.sdk.exception.SDKException;
import com.hsiaosiyuan.idea.ont.run.OntRunConfigurationOptions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class OntDeployConfig {
  public static final String FILENAME = "default-config.json";
  private static OntDeployConfig inst = null;

  public String name;
  public String version;
  public String author;
  public String email;
  public String desc;
  public Boolean needStorage;
  public String payer;
  public Integer gasPrice;
  public Integer gasLimit;

  private Project project;

  public static OntDeployConfig getInstance(Project project) throws IOException {
    if (inst != null) return inst;

    inst = new OntDeployConfig();
    inst.project = project;
    inst.load();
    return inst;
  }

  @Nullable
  public static OntDeployConfig getInstance() {
    return inst;
  }

  public Path getFilePath() {
    return Paths.get(Objects.requireNonNull(project.getBasePath()))
        .resolve("./contracts/" + FILENAME).normalize();
  }

  public byte[] getRaw() throws IOException {
    Path file = getFilePath();
    return Files.readAllBytes(file);
  }

  public void load() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw())).getJSONObject("deployConfig");
    name = obj.getString("name");
    version = obj.getString("version");
    author = obj.getString("author");
    email = obj.getString("email");
    desc = obj.getString("desc");
    needStorage = obj.getBoolean("needStorage");
    payer = obj.getString("payer");
    gasPrice = obj.getInteger("gasPrice");
    gasLimit = obj.getInteger("gasLimit");
  }

  public void save() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));
    obj.put("deployConfig", this);
    String raw = JSON.toJSONString(obj, true);
    Files.write(getFilePath(), raw.getBytes());
  }

  public void deploy(String code) throws Exception {
    OntSdk sdk = OntSdk.getInstance();

    OntRunConfigurationOptions cfg = OntRunConfigurationOptions.getInstance(project);
    assert cfg != null;

//    sdk.openWalletFile();

    sdk.setRpc(cfg.getRpcAddr());



    Transaction tx = sdk.vm().makeDeployCodeTransaction(
        code, needStorage, name, version, author, email, desc, payer, gasLimit, gasPrice);

//    sdk.signTx(tx, payer)
    String txHex = Helper.toHexString(tx.toArray());
    sdk.getRpc().syncSendRawTransaction(txHex);
  }
}
