package com.hsiaosiyuan.idea.ont.abi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

public class AbiIndexManager {
  private static AbiIndexManager manager = null;

  private AbiIndexManager() {
  }

  public static AbiIndexManager getInstance() {
    if (manager != null) return manager;
    manager = new AbiIndexManager();
    return manager;
  }

  // HashMap<src-abs-path, abi-file>
  public final HashMap<String, AbiFile> src2abi = new HashMap<>();
  public final HashSet<String> indexFileOrDirCache = new HashSet<>();

  public boolean hasFn(String srcAbs, String fn) {
    AbiFile abiFile = src2abi.get(normalizePath(srcAbs));
    if (abiFile == null) return false;

    return abiFile.hasFn(fn);
  }

  /**
   * @param file a abi file or a directory contains abi files
   */
  public void indexFileOrDir(String file) {
    if (indexFileOrDirCache.contains(file)) return;

    File f = new File(file);
    if (!f.exists()) return;

    if (f.isDirectory()) {
      indexDir(file);
    } else {
      indexFile(file);
    }

    indexFileOrDirCache.add(file);
  }

  public void indexFromSrcFile(String file, boolean force) {
    if (!file.endsWith(".py")) return;

    if (force) removeIndexBySrcFile(file);

    String filename = Paths.get(file).getFileName().toString();
    filename = filename.substring(0, filename.length() - 3);

    Path abiFile = Paths.get(file).getParent().resolve("./build/" + filename + "_abi.json");
    indexFile(abiFile.normalize().toString());
  }

  public static String normalizePath(String path) {
    return Paths.get(path).normalize().toAbsolutePath().toString();
  }

  public boolean hasSrcIndex(String src) {
    return src2abi.containsKey(normalizePath(src));
  }

  public void removeIndexBySrcFile(String file) {
    src2abi.remove(normalizePath(file));
  }

  public void removeIndexByAbiFile(String file) {
    String src = AbiFile.abiPath2SrcPath(file);
    src2abi.remove(src);
  }

  public AbiFile getAbi(String srcPath) {
    return src2abi.get(normalizePath(srcPath));
  }

  private void indexFile(String file) {
    AbiFile abiFile = AbiFile.fromFile(file);
    if (abiFile == null) return;
    src2abi.put(abiFile.getSrcPath(), abiFile);
  }

  private void indexDir(String file) {
    Stream<Path> paths;
    try {
      paths = Files.walk(Paths.get(file));
    } catch (IOException e) {
      return;
    }
    paths.filter(Files::isRegularFile).forEach((f) -> indexFile(f.toAbsolutePath().toString()));
  }
}
