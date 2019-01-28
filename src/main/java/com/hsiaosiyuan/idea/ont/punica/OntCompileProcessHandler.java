package com.hsiaosiyuan.idea.ont.punica;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.ontio.common.Address;
import com.github.ontio.common.Helper;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.hsiaosiyuan.idea.ont.run.OntProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class OntCompileProcessHandler extends OntProcessHandler {
  private static final String API = "https://smartxcompiler.ont.io/api/v2.0/python/compile";

  private void run(Project project, String srcPath) {
    OntNotifier notifier = OntNotifier.getInstance(project);

    try {
      notifyTextAvailableWithTimestamp("Compiling " + srcPath, ProcessOutputTypes.SYSTEM);

      JSONObject payload = new JSONObject();
      payload.put("type", "Python");

      String code = FileUtils.readFileToString(new File(srcPath));
      payload.put("code", code);

      HttpPost req = new HttpPost(API);
      StringEntity reqParams = new StringEntity(JSON.toJSONString(payload));
      req.setEntity(reqParams);
      req.setHeader("Content-type", "application/json");

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
      SSLContext.setDefault(ctx);

      HttpClient httpClient = HttpClientBuilder.create().setSSLContext(ctx).build();
      HttpResponse response = httpClient.execute(req);

      JSONObject resp = JSON.parseObject(new String(IOUtils.toByteArray(response.getEntity().getContent())));
      Integer errCode = resp.getInteger("errcode");
      if (errCode != 0) {
        String errMsg = resp.getString("errdetail");
        notifyTextAvailableWithTimestamp("Some errors occur: " + errMsg, ProcessOutputTypes.STDERR);
        notifyProcessTerminated(1);
        return;
      }

      String avm = resp.getString("avm");
      String abi = resp.getString("abi");

      if (avm.startsWith("b'")) {
        avm = avm.substring(2, avm.lastIndexOf("'"));
      }

      String hash = Helper.toHexString(Helper.reverse(Address.toScriptHash(Helper.hexToBytes(avm)).toArray()));
      String debug = resp.getString("debug");
      String funcMap = resp.getString("funcmap");

      saveAs(srcPath, "avm", avm);
      saveAs(srcPath, "abi", abi);
      saveAs(srcPath, "debug", debug);
      saveAs(srcPath, "funcmap", funcMap);

      notifyTextAvailableWithTimestamp("Compiled successfully, hash is " + hash, ProcessOutputTypes.SYSTEM);
      notifyProcessTerminated(0);
    } catch (Exception e) {
      String reason = e.getMessage();
      if (reason == null) reason = "Unknown error";
      notifier.notifyError("Ontology", "Failed to compile contract reason: " + reason);
      notifyTextAvailableWithTimestamp("Some errors occur", ProcessOutputTypes.STDERR);
      notifyProcessTerminated(1);
    }
  }

  private void saveAs(String srcPath, String type, String data) throws Exception {
    String dest;
    switch (type) {
      case "avm": {
        dest = AbiFile.srcPath2AvmPath(srcPath);
        break;
      }
      case "abi": {
        dest = AbiFile.srcPath2AbiPath(srcPath);
        break;
      }
      case "debug": {
        dest = AbiFile.srcPath2DebugInfoPath(srcPath);
        break;
      }
      case "funcmap": {
        dest = AbiFile.srcPath2FuncMapPath(srcPath);
        break;
      }
      default:
        throw new Exception("Unsupported type: " + type);
    }

    FileUtils.forceMkdir(Paths.get(dest).getParent().toFile());
    FileUtils.writeStringToFile(new File(dest), data);
  }

  public void start(Project project, String srcPath) {
    run(project, srcPath);
  }

  private static class DefaultTrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }

  @Override
  protected void destroyProcessImpl() {

  }

  @Override
  protected void detachProcessImpl() {

  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Nullable
  @Override
  public OutputStream getProcessInput() {
    return null;
  }

}
