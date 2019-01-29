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

  public static String avmPath2SrcPath(String avmPath) {
    Path path = Paths.get(avmPath);
    String filename = path.getFileName().toString();
    filename = filename.substring(0, filename.length() - 4);
    return Paths.get(avmPath).getParent().resolve("../" + filename + ".py").normalize().toString();
  }

  public static String srcPath2AvmPath(String srcPath) {
    String filename = extractSrcFilename(srcPath);
    return Paths.get(srcPath).getParent().resolve("./build/" + filename + ".avm").normalize().toString();
  }

  public static String srcPath2AbiPath(String srcPath) {
    String filename = extractSrcFilename(srcPath);
    return Paths.get(srcPath).getParent().resolve("./build/" + filename + "_abi.json").normalize().toString();
  }

  public static String srcPath2DebugInfoPath(String srcPath) {
    String filename = extractSrcFilename(srcPath);
    return Paths.get(srcPath).getParent().resolve("./build/" + filename + "_debug.json").normalize().toString();
  }

  public static String srcPath2FuncMapPath(String srcPath) {
    String filename = extractSrcFilename(srcPath);
    return Paths.get(srcPath).getParent().resolve("./build/" + filename + "_funcMap.json").normalize().toString();
  }

  public static String extractSrcFilename(String src) {
    Path path = Paths.get(src);
    String filename = path.getFileName().toString();
    return filename.substring(0, filename.length() - 3);
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
      return abiInfo.getFunction(name);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
