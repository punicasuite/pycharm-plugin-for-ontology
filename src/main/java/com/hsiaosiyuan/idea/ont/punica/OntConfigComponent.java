package com.hsiaosiyuan.idea.ont.punica;

import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class OntConfigComponent implements ProjectComponent, BulkFileListener {
  private final MessageBusConnection connection;

  public OntConfigComponent() {
    connection = ApplicationManager.getApplication().getMessageBus().connect();
  }

  @Override
  public void projectOpened() {
    connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
  }

  @Override
  public void projectClosed() {
    connection.disconnect();
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent evt : events) {
      if (evt instanceof VFileContentChangeEvent || evt instanceof VFileCreateEvent) {
        reloadConfigs(evt);
      }
    }
  }

  private void reloadConfigs(VFileEvent evt) {
    Project project = getProjectByFile(evt.getFile());
    if (project == null) return;

    OntNotifier notifier = OntNotifier.getInstance(project);
    try {
      reloadConfig(evt);
      reloadDeployConfig(evt);
      reloadNetworkConfig(evt);
    } catch (IOException e) {
      notifier.notifyError("Ontology", "Unable to reload config: " + e.getMessage());
    }
  }

  private void reloadConfig(VFileEvent evt) throws IOException {
    Project project = getProjectByFile(evt.getFile());
    if (project == null) return;

    Path path = OntPunicaConfig.getInstance(project).getFilePath();
    if (!path.toString().equals(evt.getPath())) return;

    OntPunicaConfig.getInstance(project).load();
  }

  private void reloadDeployConfig(VFileEvent evt) throws IOException {
    Project project = getProjectByFile(evt.getFile());
    if (project == null) return;

    Path path = OntDeployConfig.getInstance(project).getFilePath();
    if (!path.toString().equals(evt.getPath())) return;

    OntDeployConfig.getInstance(project).load();
  }

  private void reloadNetworkConfig(VFileEvent evt) throws IOException {
    Project project = getProjectByFile(evt.getFile());
    if (project == null) return;

    Path path = OntNetworkConfig.getInstance(project).getFilePath();
    if (!path.toString().equals(evt.getPath())) return;

    OntNetworkConfig.getInstance(project).load();
  }

  @Nullable
  public static Project getProjectByFile(VirtualFile file) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
        return project;
      }
    }
    return null;
  }
}
