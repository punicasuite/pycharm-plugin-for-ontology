package com.hsiaosiyuan.idea.ont.run;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OntConfigurationEditor extends SettingsEditor<OntRunConfiguration> implements ActionListener {
  private final OntRunConfiguration configuration;
  private JPanel panel1;
  private JComboBox comboBox1;
  private JLabel networkLabel;

  @SuppressWarnings("unchecked")
  public OntConfigurationEditor(Project project, OntRunConfiguration runConfiguration) {
    configuration = runConfiguration;

    comboBox1.addActionListener(this);

    runConfiguration.options.networks.forEach((key, network) -> {
      comboBox1.addItem(new Item(key, key));
    });
  }

  @Override
  protected void resetEditorFrom(@NotNull OntRunConfiguration s) {
    System.out.println(s.options.defaultNetwork);
    setSelectedValue(comboBox1, s.options.defaultNetwork);
  }

  @Override
  protected void applyEditorTo(@NotNull OntRunConfiguration s) throws ConfigurationException {
    s.options.defaultNetwork = ((Item) comboBox1.getSelectedItem()).getId();
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return panel1;
  }

  public void setSelectedValue(JComboBox comboBox, String value) {
    Item item;
    for (int i = 0; i < comboBox.getItemCount(); i++) {
      item = (Item) comboBox.getItemAt(i);
      if (item.getId().equals(value)) {
        comboBox.setSelectedIndex(i);
        break;
      }
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {

  }

  class Item {

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
  }
}
