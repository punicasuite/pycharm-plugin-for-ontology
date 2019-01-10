package com.hsiaosiyuan.idea.ont.deploy;

import com.hsiaosiyuan.idea.ont.punica.OntPunicaConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntDeployConfig;
import com.hsiaosiyuan.idea.ont.punica.config.OntNetworkConfig;
import com.hsiaosiyuan.idea.ont.run.OntNotifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class OntDeployConfigDialog extends DialogWrapper {

  private JPanel panel;
  private JTextField txName;
  private JTextField txVersion;
  private JTextField txAuthor;
  private JTextField txEmail;
  private JTextField txDesc;
  private JComboBox cbPayer;
  private JComboBox cbNetworks;

  private Project project;

  @SuppressWarnings("unchecked")
  public OntDeployConfigDialog(@Nullable Project project) throws IOException {
    super(project);
    init();

    this.project = project;

    OntDeployConfig config = OntDeployConfig.getInstance(project);
    txName.setText(config.name);
    txVersion.setText(config.version);
    txAuthor.setText(config.author);
    txEmail.setText(config.email);
    txDesc.setText(config.desc);

    for (Map.Entry<String, Object> item : OntPunicaConfig.getInstance(project).password.entrySet()) {
      cbPayer.addItem(new Item(item.getKey(), item.getKey()));
    }
    cbPayer.setSelectedItem(new Item(config.payer, config.payer));

    OntNetworkConfig netCfg = OntNetworkConfig.getInstance(project);
    netCfg.networks.keySet().forEach(k -> cbNetworks.addItem(new Item(k, k)));
    cbNetworks.setSelectedItem(new Item(netCfg.defaultNetwork, netCfg.defaultNetwork));
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();

    try {
      OntDeployConfig config = OntDeployConfig.getInstance(project);
      config.name = txName.getText().trim();
      config.version = txVersion.getText().trim();
      config.author = txAuthor.getText().trim();
      config.email = txEmail.getText().trim();
      config.desc = txDesc.getText().trim();
      config.payer = ((Item) Objects.requireNonNull(cbPayer.getSelectedItem())).getId();
      config.save();

      OntNetworkConfig netCfg = OntNetworkConfig.getInstance(project);
      netCfg.defaultNetwork = ((Item) Objects.requireNonNull(cbNetworks.getSelectedItem())).getId();
      netCfg.save();
    } catch (IOException e) {
      OntNotifier notifier = OntNotifier.getInstance(project);
      notifier.notifyError("Ontology", "Unable to save config: " + e.getMessage());
    }
  }

  public static class Item {
    private String id;
    private String description;

    public Item(String id, String description) {
      this.id = id;
      this.description = description;
    }

    public String getId() {
      return id;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Item)) return super.equals(obj);

      return ((Item) obj).getId().equals(id);
    }
  }
}
