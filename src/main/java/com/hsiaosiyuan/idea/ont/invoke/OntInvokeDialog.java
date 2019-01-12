package com.hsiaosiyuan.idea.ont.invoke;

import com.github.ontio.sdk.exception.SDKException;
import com.github.ontio.smartcontract.neovm.abi.AbiFunction;
import com.github.ontio.smartcontract.neovm.abi.Parameter;
import com.hsiaosiyuan.idea.ont.abi.AbiFile;
import com.hsiaosiyuan.idea.ont.abi.AbiIndexManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OntInvokeDialog extends DialogWrapper {
  private JPanel panel;

  private String src;
  private String method;

  private HashMap<String, OntFnParameter> parameters;
  private AbiFunction fn;

  private JCheckBox cbPreExec;
  private JCheckBox cbWaitResult;

  public OntInvokeDialog(@Nullable Project project, String src, String method) throws Exception {
    super(project);
    init();

    this.src = src;
    this.method = method;
    ((TitledBorder) panel.getBorder()).setTitle("Invoke: " + getFilename() + "::" + method);

    AbiFile abiFile = AbiIndexManager.getInstance().src2abi.get(src);
    if (abiFile == null) {
      throw new Exception("Missing ABI file: " + src);
    }

    fn = abiFile.getFn(method);
    if (fn == null) {
      throw new Exception("Unrecognized method: " + method + " in via ABI: " + src);
    }

    int pLen = fn.parameters.size();
    panel.setLayout(new GridLayoutManager(pLen + 1, 1));

    int rowIdx = 0;

    cbPreExec = new JCheckBox();
    cbPreExec.setText("Pre Exec");
    cbPreExec.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    GridConstraints c = new GridConstraints();
    c.setRow(rowIdx);
    c.setAnchor(GridConstraints.ANCHOR_WEST);
    c.setIndent(1);
    panel.add(cbPreExec, c);

    cbWaitResult = new JCheckBox();
    cbWaitResult.setText("Wait Result");
    cbWaitResult.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    c = new GridConstraints();
    c.setRow(rowIdx);
    c.setAnchor(GridConstraints.ANCHOR_WEST);
    c.setIndent(10);
    panel.add(cbWaitResult, c);

    rowIdx++;
    if (pLen > 0) {
      parameters = new HashMap<>();
      AtomicInteger i = new AtomicInteger(rowIdx);
      int len = pLen + 1;
      fn.parameters.forEach(p -> {
        OntFnParameter param = new OntFnParameter(panel, p.name);
        GridConstraints cc = new GridConstraints();
        cc.setRow(i.get());
        cc.setAnchor(GridConstraints.ANCHOR_NORTH);
        JComponent field = i.get() == len - 1 ? param.asLastField() : param.getComponent();
        panel.add(field, cc);

        parameters.put(p.name, param);
        i.getAndIncrement();
      });
    }
  }

  public AbiFunction getFn() throws OntFnParameter.InvalidTypeException, SDKException {
    AbiFunction fun = new AbiFunction();
    fun.name = fn.name;
    fun.parameters = new ArrayList<>();
    for (Parameter param : fn.parameters) {
      Parameter tp = new Parameter();
      tp.name = param.name;

      OntFnParameter pp = parameters.get(param.name);
      tp.type = pp.getType().abiType();
      tp.setValue(pp.getValue());

      fun.parameters.add(tp);
    }
    return fun;
  }

  private String getFilename() {
    Path p = Paths.get(src);
    String filename = p.getFileName().toString();
    return filename.substring(0, filename.length() - 3);
  }

  public boolean getPreExec() {
    return cbPreExec.isSelected();
  }

  public boolean getWaitResult() {
    return cbWaitResult.isSelected();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }
}
