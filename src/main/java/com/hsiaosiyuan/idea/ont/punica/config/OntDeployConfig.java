package com.hsiaosiyuan.idea.ont.punica.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.github.ontio.OntSdk;
import com.github.ontio.account.Account;
import com.github.ontio.common.Helper;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.sdk.exception.SDKException;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.github.ontio.smartcontract.neovm.abi.BuildParams;
import com.hsiaosiyuan.idea.ont.punica.OntPunicaConfig;
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

  @JSONField(serialize = false)
  public JSONObject password;

  private Project project;

  public static OntDeployConfig getInstance(Project project) throws IOException {
    if (inst == null) {
      inst = new OntDeployConfig();
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

    JSONObject deploy = obj.getJSONObject("deployConfig");
    name = deploy.getString("name");
    version = deploy.getString("version");
    author = deploy.getString("author");
    email = deploy.getString("email");
    desc = deploy.getString("desc");
    needStorage = deploy.getBoolean("needStorage");
    payer = deploy.getString("payer");
    gasPrice = deploy.getInteger("gasPrice");
    gasLimit = deploy.getInteger("gasLimit");

    password = obj.getJSONObject("password");
  }

  public void save() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));
    obj.put("deployConfig", this);
    obj.put("password", password);
    String raw = JSON.toJSONString(obj, true);
    Files.write(getFilePath(), raw.getBytes());
  }

  @Nullable
  public String getPwd(String acc) {
    return password.getString(acc);
  }

  public OntSdk prepareSdk() throws IOException {
    OntSdk sdk = OntSdk.getInstance();

    OntNetworkConfig networkConfig = OntNetworkConfig.getInstance(project);
    sdk.setRpc(networkConfig.getRpcAddr());

    OntPunicaConfig punicaConfig = OntPunicaConfig.getInstance(project);
    sdk.openWalletFile(punicaConfig.getWalletPath().toString());
    return sdk;
  }

  public Object sendTx(Transaction tx, boolean preExec, boolean wait) throws Exception {
    OntSdk sdk = prepareSdk();
    sdk.signTx(tx, new Account[][]{{sdk.getWalletMgr().getAccount(payer, getPwd(payer))}});
    String txHex = Helper.toHexString(tx.toArray());
    if (wait) {
      return sdk.getRpc().sendRawTransactionSync(txHex);
    }
    if (preExec) {
      return sdk.getRpc().sendRawTransactionPreExec(txHex);
    }
    return sdk.getRpc().sendRawTransaction(txHex);
  }

  public Object deploy(String code) throws Exception {
    OntSdk sdk = prepareSdk();

    // TODO:: needStorage
    Transaction tx = sdk.vm().makeDeployCodeTransaction(
        code, true, name, version, author, email, desc, payer, gasLimit, gasPrice);

    return sendTx(tx, false, true);
  }

  public Object invoke(String contract, AbiFunction fn, boolean preExec, boolean wait) throws Exception {
    OntSdk sdk = prepareSdk();

    byte[] params = BuildParams.serializeAbiFunction(fn);
    Transaction tx = sdk.vm().makeInvokeCodeTransaction(
        Helper.reverse(contract),
        null,
        params,
        payer,
        gasLimit,
        gasPrice);

    return sendTx(tx, preExec, wait);
  }
}
