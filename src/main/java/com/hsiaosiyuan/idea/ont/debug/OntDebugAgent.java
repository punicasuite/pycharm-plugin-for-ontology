package com.hsiaosiyuan.idea.ont.debug;

import com.alibaba.fastjson.JSON;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.hsiaosiyuan.idea.ont.invoke.OntInvokeDialog;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.hsiaosiyuan.idea.ont.run.OntRunCmdHandler;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OntDebugAgent {
  private XDebugSession session;
  private Project project;
  private String ticket;
  private Listener listener;
  private OSProcessHandler processHandler;
  private ConsoleView consoleView;
  private Socket socket;
  private ArrayList<XBreakpoint> breakpoints;
  private HashMap<String, Pending> pendings;
  private String src;
  private String method;
  private ExecutorService executorService;

  private JSONObject params;

  public OntDebugAgent(XDebugSession session, ConsoleView consoleView, String src, String method) {
    this.session = session;
    this.project = session.getProject();
    this.consoleView = consoleView;
    this.src = src;
    this.method = method;
    ticket = UUID.randomUUID().toString();
    breakpoints = new ArrayList<>();
    pendings = new HashMap<>();
    executorService = Executors.newFixedThreadPool(2);
  }

  @Nullable
  public Path getLockFilePath() {
    String tmp = System.getProperty("java.io.tmpdir");
    if (tmp == null) return null;

    return Paths.get(tmp).resolve("./ontdev-debug-" + ticket + ".lock").normalize();
  }

  @Nullable
  public LockContent getLockContent() {
    Path path = getLockFilePath();
    if (path == null) return null;
    try {
      return JSON.parseObject(Files.readAllBytes(path), LockContent.class);
    } catch (IOException e) {
      return null;
    }
  }

  public boolean releaseLock() {
    Path path = getLockFilePath();
    if (path == null) return false;

    File file = path.toFile();
    if (file == null) return false;

    return file.delete();
  }

  private File getLockFile() throws Exception {
    Path lockPath = getLockFilePath();
    if (lockPath == null) throw new Exception("Unable to determine lock path");

    File lockFile = lockPath.toFile();
    if (lockFile == null) throw new Exception("Unable to determine lock file");

    return lockFile;
  }

  public void start(@Nullable Listener listener) throws Exception {
    Path lockPath = getLockFilePath();
    if (lockPath == null) throw new Exception("Unable to determine lock path");


    File lockFile = lockPath.toFile();
    if (lockFile != null && lockFile.exists()) {
      throw new Exception("Lock file already exist: " + lockPath.toAbsolutePath().toString());
    }

    this.listener = listener;

    GeneralCommandLine cmd = new GeneralCommandLine();
    cmd.setExePath("/Users/hsy/ws/ont/ontdev/bin/ontdev.js");
    cmd.addParameters("debug");
    cmd.addParameter("--ticket");
    cmd.addParameter(ticket);

    processHandler = new OntRunCmdHandler(cmd.createProcess(), cmd.getCommandLineString());
    consoleView.attachToProcess(processHandler);

    processHandler.startNotify();

    OntNotifier notifier = OntNotifier.getInstance(project);

    final OntDebugAgent agent = this;
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Starting Debug Server") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          indicator.setIndeterminate(true);

          Thread.sleep(2000);

          File lockFile = getLockFile();
          if (lockFile.exists()) {
            if (agent.listener != null) {
              LockContent lockContent = getLockContent();
              agent.listener.onServerStarted(agent, lockContent);
            }
            agent.connect();
          } else {
            throw new Exception("Unable to start debug server");
          }
        } catch (Exception e) {
          notifier.notifyError("Ontology", e);
        }
        indicator.setFraction(1);
      }
    });
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void stop() throws Exception {
    processHandler.getProcess().destroyForcibly();
    getLockFile().delete();
    if (socket != null) {
      socket.disconnect();
    }
    for (String key : pendings.keySet()) {
      Pending item = pendings.get(key);
      if (item != null) {
        item.future.complete(new JSONObject());
      }
    }
  }

  @Nullable
  private Integer getPort() {
    LockContent lockContent = getLockContent();
    if (lockContent == null) return null;
    return lockContent.port;
  }

  private OntNotifier getNotifier() {
    return OntNotifier.getInstance(project);
  }

  private void connect() throws Exception {
    Integer port = getPort();
    if (port == null) {
      throw new Exception("Unable to get server port");
    }

    socket = IO.socket("http://localhost:" + port);
    final OntDebugAgent agent = this;

    socket.on(Socket.EVENT_CONNECT, args -> {
      if (listener != null) {
        listener.onReady(this);
      }

      // submit task to thread pool since Event Loop looks like being blocked by Future::get()
      executorService.submit(() -> {
        try {
          setupAndStart();
        } catch (Exception e) {
          getNotifier().notifyError("Ontology", e);
        }
      });
    });

    socket.on("RESP", args -> {
      try {
        JSONObject obj = (JSONObject) args[0];
        String id = obj.getString("id");
        Pending pending = pendings.get(id);
        if (pending != null) {
          int err = obj.getInt("error");
          if (err != 0) {
            pending.future.cancel(false);
          } else {
            pending.future.complete(obj);
          }
          pendings.remove(id);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
    });

    socket.on("BP", args -> {
      JSONObject obj = (JSONObject) args[0];
      SwingUtilities.invokeLater(() -> {
        XBreakpoint bp = findBreakpoint(obj);
        if (bp != null) {
          session.breakpointReached(bp, null, new OntSuspendContext(bp.getSourcePosition(), agent));
        }
      });
    });

    socket.on("STEP", args -> {
      JSONObject obj = (JSONObject) args[0];
      try {
        int line = obj.getInt("file_line_no");
        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(Paths.get(src).toFile());
        XSourcePosition position = XDebuggerUtil.getInstance().createPosition(vf, line - 1);
        SwingUtilities.invokeLater(() -> {
          session.positionReached(new OntSuspendContext(position, agent));
        });
      } catch (JSONException e) {
        e.printStackTrace();
      }
    });

    socket.on("END", args -> {
      SwingUtilities.invokeLater(() -> {
        session.stop();
      });
    });

    socket.connect();
  }

  public CompletableFuture<JSONObject> send(String evt, JSONObject data, boolean wait) {
    String id = UUID.randomUUID().toString();
    try {
      data.put("id", id);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    socket.emit(evt, data);
    if (wait) {
      Pending pending = new Pending(id);
      pendings.put(id, pending);
      return pending.future;
    }
    return CompletableFuture.completedFuture(new JSONObject());
  }

  private CompletableFuture<JSONObject> init() throws Exception {
    JSONObject obj = new JSONObject();
    AbiFile abiFile = AbiIndexManager.getInstance().src2abi.get(src);
    if (abiFile == null) {
      throw new Exception("Unable to find ABI file");
    }
    obj.put("sourceFile", src);
    obj.put("contractHash", abiFile.hash);
    obj.put("avm", AbiFile.srcPath2AvmPath(src));
    obj.put("abi", AbiFile.srcPath2AbiPath(src));
    obj.put("debugInfo", AbiFile.srcPath2DebugInfoPath(src));
    obj.put("funcMap", AbiFile.srcPath2FuncMapPath(src));
    return send("init", obj, true);
  }

  public void addBreakpoint(XBreakpoint breakpoint) {
    breakpoints.add(breakpoint);
    if (socket != null && socket.connected()) {
      setBreakpoints();
    }
  }

  public void removeBreakpoint(XBreakpoint breakpoint) {
    breakpoints.remove(breakpoint);
    if (socket != null && socket.connected()) {
      setBreakpoints();
    }
  }

  @Nullable
  private XBreakpoint findBreakpoint(JSONObject bp) {
    try {
      int line = bp.getInt("line");
      for (XBreakpoint p : breakpoints) {
        if (p.getSourcePosition().getLine() == line) {
          return p;
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  private JSONObject makeBreakpoints() {
    JSONObject ret = new JSONObject();

    for (XBreakpoint p : breakpoints) {
      try {
        String path = p.getSourcePosition().getFile().getPath();
        JSONArray ps;
        if (ret.has(path)) {
          ps = ret.getJSONArray(path);
        } else {
          ps = new JSONArray();
          ret.put(path, ps);
        }
        ps.put(p.getSourcePosition().getLine());
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    return ret;
  }

  private CompletableFuture<JSONObject> setBreakpoints() {
    JSONObject data = new JSONObject();
    try {
      data.put("points", makeBreakpoints());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return send("set-breakpoints", data, true);
  }

  private CompletableFuture<JSONObject> startMethod() {
    JSONObject data = new JSONObject();
    try {
      data.put("method", method);
      data.put("data", params);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return send("start", data, true);
  }

  private CompletableFuture<String> showDialog() {
    CompletableFuture<String> future = new CompletableFuture<>();
    SwingUtilities.invokeLater(() -> {
      OntInvokeDialog invokeDialog;
      try {
        invokeDialog = new OntInvokeDialog(project, src, method);
      } catch (Exception e) {
        getNotifier().notifyError("Ontology", e);
        future.complete("Unable to make invoke dialog");
        return;
      }

      if (!invokeDialog.showAndGet()) {
        future.complete("User canceled");
        return;
      }

      params = invokeDialog.getDebugParams();
      future.complete(null);
    });
    return future;
  }

  private void setupAndStart() throws Exception {
    String errMsg = showDialog().get();

    if (errMsg != null) {
      if (errMsg.equals("User canceled")) {
        processHandler.notifyTextAvailable("User canceled\n", ProcessOutputTypes.SYSTEM);
        session.stop();
      } else {
        getNotifier().notifyError("Ontology", errMsg);
      }
      return;
    }

    init()
        .thenApply((ret) -> setBreakpoints())
        .thenApply((ret) -> startMethod()).get();
  }

  public void resume() {
    try {
      send("continue", new JSONObject(), false).get();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stepOver() {
    try {
      send("next", new JSONObject(), false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stepInto() {
    try {
      send("stepIn", new JSONObject(), false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stepOut() {
    try {
      send("stepOut", new JSONObject(), false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public CompletableFuture<JSONObject> queryVariables(@Nullable String rf) {
    JSONObject data = new JSONObject();
    if (rf == null) rf = "0";
    try {
      data.put("rf", rf);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return send("variables", data, true);
  }

  public static class LockContent {
    public Integer port;
  }

  public static abstract class Listener {
    public abstract void onServerStarted(OntDebugAgent agent, LockContent lockContent);

    public abstract void onReady(OntDebugAgent agent);
  }

  public static class Pending {
    public String id;
    public CompletableFuture<JSONObject> future;

    public Pending(String id) {
      this.id = id;
      future = new CompletableFuture<>();
    }
  }
}
