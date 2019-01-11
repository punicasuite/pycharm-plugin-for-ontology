package com.hsiaosiyuan.idea.ont.abi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.github.ontio.smartcontract.neovm.abi.AbiInfo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class AbiFile {
  public String CompilerVersion;
  public String hash;
  public String entrypoint;
  public ArrayList<AbiFn> functions;

  private String selfPath;
  private String srcPath;

  private AbiInfo abiInfo = null;

  public static boolean isAbiFile(String file) {
    Path path = Paths.get(file);
    return path.toString().endsWith("abi.json");
  }

  @Nullable
  public static AbiFile fromFile(String file) {
    if (!isAbiFile(file)) return null;

    byte[] raw;
    try {
      raw = Files.readAllBytes(Paths.get(file));
    } catch (IOException e) {
      return null;
    }

    AbiFile inst = JSON.parseObject(raw, AbiFile.class);
    inst.selfPath = file;
    inst.srcPath = abiPath2SrcPath(file);
    return inst;
  }

  public static String abiPath2SrcPath(String abiPath) {
    Path path = Paths.get(abiPath);
    String filename = path.getFileName().toString();
    filename = filename.substring(0, filename.length() - 9);
    return Paths.get(abiPath).getParent().resolve("../" + filename + ".py").normalize().toString();
  }

  public static String srcPath2AvmPath(String srcPath) {
    Path path = Paths.get(srcPath);
    String filename = path.getFileName().toString();
    filename = filename.substring(0, filename.length() - 3);
    return Paths.get(srcPath).getParent().resolve("./build/" + filename + ".avm").normalize().toString();
  }

  @JSONField(serialize = false)
  public String getSelfPath() {
    return selfPath;
  }

  @JSONField(serialize = false)
  public String getSrcPath() {
    return srcPath;
  }

  public boolean hasFn(String fn) {
    return functions.stream().anyMatch((f) -> f.name.equals(fn));
  }

  public void destroy() {
    AbiIndexManager.getInstance().src2abi.remove(srcPath);
    deleteFiles();
  }

  public void deleteFiles() {
    deleteAbiFile();
    deleteAvmFile();
  }

  public void deleteAbiFile() {
    new File(selfPath).delete();
  }

  public void deleteAvmFile() {
    String src = abiPath2SrcPath(selfPath);
    new File(srcPath2AvmPath(src)).delete();
  }

  @Nullable
  public AbiFunction getFn(String name) {
    if (abiInfo != null) return abiInfo.getFunction(name);

    try {
      byte[] raw = Files.readAllBytes(Paths.get(selfPath));
      abiInfo = JSON.parseObject(raw, AbiInfo.class);
      return  abiInfo.getFunction(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
