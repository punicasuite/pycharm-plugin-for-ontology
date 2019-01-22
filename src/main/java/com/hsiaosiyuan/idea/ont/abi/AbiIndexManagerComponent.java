package com.hsiaosiyuan.idea.ont.abi;

import com.hsiaosiyuan.idea.ont.punica.OntConfigComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class AbiIndexManagerComponent implements ProjectComponent, BulkFileListener {
  private final MessageBusConnection connection;

  public AbiIndexManagerComponent() {
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
      if (evt instanceof VFileContentChangeEvent) {
        removeIndex(evt);
      }
    }
  }

  private void removeIndex(VFileEvent evt) {
    Project project = OntConfigComponent.getProjectByFile(evt.getFile());
    if (project == null) return;

    String path = Objects.requireNonNull(evt.getFile()).getPath();
    AbiFile idx = AbiIndexManager.getInstance().getAbi(path);
    if (idx == null) return;

    idx.destroy();

    ApplicationManager.getApplication().invokeLater(() -> {
      VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
    });
  }
}
