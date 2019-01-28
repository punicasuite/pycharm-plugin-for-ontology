package com.hsiaosiyuan.idea.ont.webview;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.webkit.WebConsoleListener;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;

public abstract class OntWebView extends JPanel {
  private Stage stage;
  protected WebView browser;
  private JFXPanel jfxPanel;
  private WebEngine webEngine;

  private WeakReference<JDialog> weakDialog;

  public OntWebView() {
    initComponents();
  }

  abstract public String getTitle();

  abstract public String getUrl();

  abstract public void onCreateScene(WebView webView, WebEngine webEngine);

  private void initComponents() {
    jfxPanel = new JFXPanel();
    createScene();

    setLayout(new BorderLayout());
    add(jfxPanel, BorderLayout.CENTER);
  }

  private void createScene() {
    PlatformImpl.startup(() -> {
      stage = new Stage();
      stage.setResizable(false);

      StackPane root = new StackPane();
      Scene scene = new Scene(root, 640, 480);
      stage.setScene(scene);

      browser = new WebView();
      webEngine = browser.getEngine();

      webEngine.load(getUrl());

      ObservableList<Node> children = root.getChildren();
      children.add(browser);

      jfxPanel.setScene(scene);

      WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
        System.out.println("Console: [" + sourceId + ":" + lineNumber + "] " + message);
      });

      onCreateScene(browser, webEngine);
    });
  }

  public void showAndWait() {
    if (weakDialog != null && weakDialog.get() != null) return;

    JDialog dialog = new JDialog();
    dialog.setModal(true);

    dialog.getContentPane().add(this);
    dialog.setTitle(getTitle());
    dialog.setResizable(false);
    dialog.setMinimumSize(new Dimension(640, 480));

    weakDialog = new WeakReference<>(dialog);
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);
  }

  public void closeDialog() {
    JDialog d = weakDialog.get();
    if (d == null) return;

    d.setVisible(false);
    weakDialog.clear();
  }

  public void callJS(String method, Object... arguments) {
    Platform.runLater(() -> {
      assert webEngine != null;
      Object methodRef = webEngine.executeScript("window." + method);
      assert methodRef != null;
      JSObject win = (JSObject) webEngine.executeScript("window");
      win.call(method, arguments);
    });
  }
}
