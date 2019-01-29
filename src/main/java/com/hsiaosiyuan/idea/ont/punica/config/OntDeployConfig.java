package com.hsiaosiyuan.idea.ont.punica.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.github.ontio.OntSdk;
import com.github.ontio.account.Account;
import com.github.ontio.common.Helper;
import com.github.ontio.core.transaction.Transaction;
import com.hsiaosiyuan.idea.ont.punica.OntPunicaConfig;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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

  public void reload(String path) throws IOException {
    path = Paths.get(path).normalize().toString();
    if (!getFilePath().toString().equals(path)) return;
    load();
  }

  public void save() throws IOException {
    JSONObject obj = JSON.parseObject(new String(getRaw()));
    obj.put("deployConfig", this);
    obj.put("password", password);
    String raw = JSON.toJSONString(obj, true);
    Files.write(getFilePath(), raw.getBytes());
  }

  public String getPwd(String acc) {
    String pwd = password.getString(acc);
    if (pwd != null && !pwd.equals("")) return pwd;

    Semaphore sm = new Semaphore();
    sm.down();

    AtomicReference<String> password = new AtomicReference<>("");

    SwingUtilities.invokeLater(() -> {
      OntPasswordDialog dialog = new OntPasswordDialog(project, acc);
      if (dialog.showAndGet()) {
        password.set(dialog.getPassword());
      }
      sm.up();
    });

    sm.waitFor();
    return password.get();
  }

  @Nullable
  public static OntSdk prepareSdk(Project project) throws IOException {
    OntPunicaConfig punicaConfig = OntPunicaConfig.getInstance(project);
    Path walletPath = punicaConfig.getWalletPath();

    if (!walletPath.toFile().exists()) {
      OntNotifier notifier = OntNotifier.getInstance(project);
      notifier.notifyError("Ontology", "Unable to find wallet file: " + walletPath.toString());
      return null;
    }

    OntSdk sdk = OntSdk.getInstance();

    OntNetworkConfig networkConfig = OntNetworkConfig.getInstance(project);
    sdk.setRpc(networkConfig.getRpcAddr());

    sdk.openWalletFile(walletPath.toString());
    return sdk;
  }

  public Object deploy(String code) throws Exception {
    OntSdk sdk = prepareSdk(project);

    if (sdk == null) return null;

    Transaction tx = sdk.vm().makeDeployCodeTransaction(
        code, needStorage, name, version, author, email, desc, payer, gasLimit, gasPrice);

    String pwd = getPwd(payer);
    if (pwd.equals("")) {
      throw new Exception("Unable to get password for account: " + payer);
    }
    return sendTx(project, tx, false, true, payer, pwd);
  }

  public static Object sendTx(Project project, Transaction tx, boolean preExec, boolean wait, String payer, String pwd) throws Exception {
    OntSdk sdk = prepareSdk(project);
    if (sdk == null) return null;

    try {
      sdk.getWalletMgr().getAccount(payer, pwd);
    } catch (Exception e) {
      e.printStackTrace();
      OntNotifier notifier = OntNotifier.getInstance(project);
      notifier.notifyError("Ontology", "Unable to get account: " + payer +
          ", please make sure the account is listed in wallet file and the specified password is right");
      return null;
    }

    sdk.signTx(tx, new Account[][]{{sdk.getWalletMgr().getAccount(payer, pwd)}});
    String txHex = Helper.toHexString(tx.toArray());
    if (wait) {
      return sdk.getRpc().sendRawTransactionSync(txHex);
    }
    if (preExec) {
      return sdk.getRpc().sendRawTransactionPreExec(txHex);
    }
    return sdk.getRpc().sendRawTransaction(txHex);
  }
}
