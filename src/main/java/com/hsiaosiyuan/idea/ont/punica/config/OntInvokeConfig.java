package com.hsiaosiyuan.idea.ont.punica.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.github.ontio.OntSdk;
import com.github.ontio.common.Helper;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.github.ontio.smartcontract.neovm.abi.BuildParams;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class OntInvokeConfig {
  public static final String FILENAME = "default-config.json";
  private static OntInvokeConfig inst = null;

  public String defaultPayer;
  public Integer gasPrice;
  public Integer gasLimit;

  private Project project;

  public static OntInvokeConfig getInstance(Project project) throws IOException {
    if (inst == null) {
      inst = new OntInvokeConfig();
    }

    inst.project = project;
    inst.load();
    return inst;
  }

  public Path getFilePath() {
    return Paths.get(Objects.requireNonNull(project.getBasePath()))
        .resolve("./contracts/" + FILENAME).normalize();
  }

  @JSONField(serialize = false)
  public byte[] getRaw() throws IOException {
    Path file = getFilePath();
    return Files.readAllBytes(file);
  }

  public void load() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));

    JSONObject deploy = obj.getJSONObject("invokeConfig");

    gasPrice = deploy.getInteger("gasPrice");
    gasLimit = deploy.getInteger("gasLimit");
    defaultPayer = deploy.getString("defaultPayer");
  }

  public void reload(String path) throws IOException {
    path = Paths.get(path).normalize().toString();
    if (!getFilePath().toString().equals(path)) return;
    load();
  }

  public void save() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));
    obj.put("invokeConfig", this);
    String raw = JSON.toJSONString(obj, true);
    Files.write(getFilePath(), raw.getBytes());
  }

  public Object invoke(
      String contract,
      AbiFunction fn,
      boolean preExec,
      boolean wait) throws Exception {
    
    OntSdk sdk = OntDeployConfig.prepareSdk(project);
    if (sdk == null) return null;

    String pwd = OntDeployConfig.getInstance(project).getPwd(defaultPayer);
    if (pwd.equals("")) {
      throw new Exception("Unable to get password for account: " + defaultPayer);
    }

    byte[] params = BuildParams.serializeAbiFunction(fn);
    Transaction tx = sdk.vm().makeInvokeCodeTransaction(
        Helper.reverse(contract),
        null,
        params,
        defaultPayer,
        gasLimit,
        gasPrice);

    return OntDeployConfig.sendTx(project, tx, preExec, wait, defaultPayer, pwd);
  }
}
