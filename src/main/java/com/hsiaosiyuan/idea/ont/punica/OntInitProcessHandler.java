package com.hsiaosiyuan.idea.ont.punica;

import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.hsiaosiyuan.idea.ont.run.OntProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.jgit.api.Git;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class OntInitProcessHandler extends OntProcessHandler {
  private static final String REPO = "https://gitee.com/punica-box/init-box";

  private void run(Project project) {
    OntNotifier notifier = OntNotifier.getInstance(project);
    try {
      Path tmpDir = Files.createTempDirectory("ont");
      notifyTextAvailableWithTimestamp("Cloning into temporary directory " + tmpDir.toAbsolutePath().toString(), ProcessOutputTypes.SYSTEM);

      Git.cloneRepository()
          .setURI(REPO)
          .setDirectory(tmpDir.toFile())
          .call();

      notifyTextAvailableWithTimestamp("Copying into project directory...", ProcessOutputTypes.SYSTEM);

      File src = tmpDir.toFile();
      File dest = new File(Objects.requireNonNull(project.getBasePath()));

      IOFileFilter gitFilter = FileFilterUtils.nameFileFilter(".git");
      FileFilter filter = FileFilterUtils.notFileFilter(gitFilter);

      FileUtils.copyDirectory(src, dest, filter);

      notifyTextAvailableWithTimestamp("Project initialized successfully, enjoy!", ProcessOutputTypes.SYSTEM);
      notifyProcessTerminated(0);
    } catch (Exception e) {
      String reason = e.getMessage();
      if (reason == null) reason = "Unknown error";
      notifier.notifyError("Ontology", "Failed to init project, reason: " + reason);
      notifyTextAvailableWithTimestamp("Some errors occur", ProcessOutputTypes.STDERR);
      notifyProcessTerminated(1);
    }
  }

  public void start(Project project) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      run(project);
    });
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
