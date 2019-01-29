$(function() {
  function NOOP() {}

  var idGenerator = (function() {
    var id = 0;
    return function() {
      return id++;
    };
  })();

  var fmt = function(str, obj) {
    var args = arguments;

    return str.replace(/\{([0-9]+)\}/g, function() {
      return args[parseInt(arguments[1]) + 1];
    });
  };

  var resolveIconPath = function(path) {
    return "../assets/editor/imgs/" + path;
  };

  var ellipsisStr = function(str, len) {
    len = len || 6;
    if (str.length > len * 2) {
      return str.substr(0, 5) + "..." + str.substr(-5);
    }
    return str;
  };

  var typeIcon = function(type) {
    var icon = "blue.gif";
    switch (type) {
      case "String":
      case "Integer":
      case "Address":
      case "Boolean":
      case "ByteArray": {
        if (type === "Integer") {
          icon = "green.gif";
        } else if (type === "Boolean") {
          icon = "yellow.gif";
        }
        break;
      }
      case "Array": {
        icon = "array.gif";
        break;
      }
      case "Map": {
        icon = "object.gif";
        break;
      }
    }
    return resolveIconPath(icon);
  };

  var paramToNode = function(params, pId) {
    pId = pId || 0;

    if (params.id === undefined) {
      params.id = idGenerator();
    }

    switch (params.type) {
      case "String":
      case "Integer":
      case "Address":
      case "Boolean":
      case "ByteArray": {
        var icon = "blue.gif";
        if (params.type === "Integer") {
          icon = "green.gif";
        } else if (params.type === "Boolean") {
          icon = "yellow.gif";
        }
        return {
          id: params.id,
          pId: pId,
          name: params.name,
          title: params.type + ": " + params.value,
          dataType: params.type,
          data: params.value,
          icon: resolveIconPath(icon)
        };
      }
      case "Array": {
        var items = params.value;
        var children = [];
        for (var i = 0, len = items.length; i < len; ++i) {
          var item = items[i];
          item.name = i + ": " + ellipsisStr(item.value);
          children.push(paramToNode(item, params.id));
        }
        return {
          id: params.id,
          pId: pId,
          name: params.name,
          title: "Array: " + params.name,
          icon: resolveIconPath("array.gif"),
          open: true,
          dataType: params.type,
          children: children
        };
      }
      case "Map": {
        var items = params.value;
        var children = [];
        for (var p in items) {
          if (items.hasOwnProperty(p)) {
            var item = items[p];
            item.name = p;
            if (["Array", "Map"].indexOf(item.type) === -1) {
              item.name += ": " + ellipsisStr(item.value);
            }
            children.push(paramToNode(items[p], params.id));
          }
        }
        return {
          id: params.id,
          pId: pId,
          name: params.name,
          title: pId === -1 ? "" : "Map: " + params.name,
          icon: resolveIconPath("object.gif"),
          open: true,
          dataType: params.type,
          children: children
        };
      }
      default:
        throw new Error("Unsupported type: " + params.type);
    }
  };

  var nodeToParam = function(node) {
    switch (node.dataType) {
      case "String":
      case "Integer":
      case "Address":
      case "Boolean":
      case "ByteArray": {
        return {
          type: node.dataType,
          value: node.data.toString()
        };
      }
      case "Array": {
        var children = [];
        var ret = {
          type: "Array",
          value: children
        };
        var cs = node.children || [];
        for (var i = 0, len = cs.length; i < len; ++i) {
          children.push(nodeToParam(cs[i]));
        }
        return ret;
      }
      case "Map": {
        var children = {};
        var ret = {
          type: "Map",
          value: children
        };
        var cs = node.children || [];
        for (var i = 0, len = cs.length; i < len; ++i) {
          var k = cs[i].name.split(":")[0];
          children[k] = nodeToParam(cs[i]);
        }
        return ret;
      }
      default:
        throw new Error("Unsupported type: " + node.dataType);
    }
  };

  // class AddChildButton
  function AddChildButton(cb) {
    this.cb = cb;
  }

  AddChildButton.prototype.show = function(treeNode) {
    if (treeNode.level === 0) return;

    var id = "btn-add-child-" + treeNode.tId;
    var selector = "#" + id;
    if (["Array", "Map"].indexOf(treeNode.dataType) === -1) {
      $(selector).hide();
      return;
    }

    if ($(selector).length > 0) {
      $(selector).show();
      return;
    }

    var nodeEl = $("#" + treeNode.tId + "_span");
    var html = fmt(
      "<span class='button add' id='{0}'><i class='icon-plus-sign'></i></span>",
      id
    );
    nodeEl.after(html);

    var me = this;
    $(selector).on("click", function() {
      me.cb(treeNode);
    });
  };

  AddChildButton.prototype.hide = function(treeNode) {
    var id = "btn-add-child-" + treeNode.tId;
    var selector = "#" + id;
    $(selector).hide();
  };

  // class DeleteButton
  function DeleteButton(cb) {
    this.cb = cb;
  }

  DeleteButton.prototype.show = function(treeNode) {
    if (treeNode.level === 0 || treeNode.level === 1) return;

    var id = "btn-del-child-" + treeNode.tId;
    var selector = "#" + id;
    if ($(selector).length > 0) {
      $(selector).show();
      return;
    }

    var nodeEl = $("#" + treeNode.tId + "_span");
    var html = fmt(
      "<span class='button del' id='{0}'><i class='icon-remove-sign'></i></span>",
      id
    );
    nodeEl.after(html);

    var me = this;
    $(selector).on("click", function() {
      return me.cb(treeNode);
    });
  };

  DeleteButton.prototype.hide = function(treeNode) {
    var id = "btn-del-child-" + treeNode.tId;
    var selector = "#" + id;
    $(selector).hide();
  };

  // class DragDrop
  function DragDrop(elem, cbs) {
    this.elem = elem;
    this.isInitialized = false;

    this.isMoving = false;
    this.point = { x: 0, y: 0 };
    this.delta = { x: 0, y: 0 };

    var defaultCbs = {
      onStart: NOOP,
      onMove: NOOP,
      onStop: NOOP
    };

    this.cbs = $.extend({}, defaultCbs, cbs);
  }

  DragDrop.prototype._capturePoint = function(evt) {
    this.point = {
      x: evt.clientX,
      y: evt.clientY
    };
  };

  DragDrop.prototype._onMouseDown = function(evt) {
    this.isMoving = true;
    this._capturePoint(evt);
    this.cbs.onStart(this);
  };

  DragDrop.prototype._onMouseMove = function(evt) {
    if (!this.isMoving) return;

    this.delta = {
      x: evt.clientX - this.point.x,
      y: evt.clientY - this.point.y
    };
    this._capturePoint(evt);
    this.cbs.onMove(this);
  };

  DragDrop.prototype._onMouseUp = function(evt) {
    if (!this.isMoving) return;

    this.isMoving = false;
    this.delta = {
      x: evt.clientX - this.point.x,
      y: evt.clientY - this.point.y
    };
    this.cbs.onStop(this);
  };

  DragDrop.prototype.init = function() {
    if (this.isInitialized) return;

    this.elem.on("mousedown", this._onMouseDown.bind(this));
    $(document).on("mousemove", this._onMouseMove.bind(this));
    $(document).on("mouseup", this._onMouseUp.bind(this));
    this.isInitialized = true;
  };

  // class NodeEditor
  function NodeEditor(table, editor) {
    this.table = table;
    this.editor = editor;
    this.isInitialized = false;

    this.typeElem = this.table.find("select");
    this.Types = {
      String: "String",
      Integer: "Integer",
      Address: "Address",
      Boolean: "Boolean",
      ByteArray: "ByteArray",
      Array: "Array",
      Map: "Map"
    };

    this.nameElem = this.table.find("[name='name']");
    this.typeElem = this.table.find("[name='type']");
    this.valElem = this.table.find("[name='value']");
    this.boolElem = this.table.find("[name='bool']");

    this.nameLabelElem = this.table.find(".name-label");
    this.typeLabelElem = this.table.find(".type-label");
    this.valLabelElem = this.table.find(".val-label");

    this.btnApply = this.table.find(".btn-apply");

    this.node = null;
  }

  NodeEditor.prototype._installTypes = function() {
    var html = "";
    for (var p in this.Types) {
      if (this.Types.hasOwnProperty(p)) {
        html += fmt("<option value='{0}'>{1}</option>", p, p);
      }
    }
    this.typeElem.html(html);

    this.typeElem.on(
      "change",
      function() {
        var v = this.typeElem.val();
        this._showBool(v === "Boolean");
        var isCompound = ["Map", "Array"].indexOf(v) !== -1;
        this.setValueReadOnly(isCompound);
        if (isCompound) {
          this.valElem.val("0 children");
        } else {
          this.valElem.val("");
        }
        this.table.find(".err").hide();
      }.bind(this)
    );
  };

  NodeEditor.prototype._activeBtnApply = function() {
    this.btnApply.on("click", this._onBtnApplyClick.bind(this));
  };

  NodeEditor.prototype._updateTree = function() {
    var name = this.nameElem.val();
    var type = this.typeElem.val();
    var val = this.valElem.val();

    if (["Array", "Map"].indexOf(type) !== -1) {
      this.node.name = name;
      this.node.title = type + ": " + name;
    } else {
      if (type === "Boolean") {
        val = this.table.find("[name='bool']:checked").val();
      }
      this.node.name = name + ": " + ellipsisStr(val);
      this.node.title = type + ": " + val;
      if (this.node.isParent) {
        this.editor.zTree.removeChildNodes(this.node);
        this.editor.addChildBtn.hide(this.node);
      }
    }
    this.node.data = val;
    this.node.dataType = type;
    this.node.icon = typeIcon(type);
    this.editor.zTree.updateNode(this.node);
  };

  NodeEditor.prototype._onBtnApplyClick = function() {
    if (this.node === null) return;

    this._validateForm();
    if (this.table.find(".err").is(":visible")) return;
    this._updateTree();
  };

  NodeEditor.prototype.init = function() {
    if (this.isInitialized) return;

    this._installTypes();
    this._showBool(false);
    this._activeBtnApply();
  };

  NodeEditor.prototype.setNameReadOnly = function(flag) {
    if (flag) {
      this.nameElem.attr("readonly", "readonly");
    } else {
      this.nameElem.removeAttr("readonly");
    }
  };

  NodeEditor.prototype.setValueReadOnly = function(flag) {
    if (flag) {
      this.valElem.attr("readonly", "readonly");
    } else {
      this.valElem.removeAttr("readonly");
    }
  };

  NodeEditor.prototype._showBool = function(flag) {
    if (flag) {
      this.valElem.hide();
      this.boolElem.parents("label").show();
    } else {
      this.valElem.show();
      this.boolElem.parents("label").hide();
    }
    this.editor.refreshDecorates();
  };

  NodeEditor.prototype._validateForm = function() {
    this._validate(this._validateName, this.nameElem);
    this._validate(this._validateValue, this.valElem);
  };

  NodeEditor.prototype._validate = function(predicate, elem) {
    if (this.node === null) return;

    var errElem = elem.next(".err");
    if (errElem.length === 0) {
      elem.after("<div class='err'></div>");
      errElem = elem.next(".err");
    }

    errElem.hide();
    var state = predicate.call(this, elem.val().trim());
    if (state !== true) {
      errElem.html(state).show();
    }
  };

  NodeEditor.prototype._validateName = function(v) {
    if (v.length === 0) {
      return "Please specify key";
    }
    var pNode = this.node.getParentNode();
    if (pNode.dataType === "Map") {
      var children = pNode.children;
      var name = v;
      for (var i = 0, len = children.length; i < len; ++i) {
        var child = children[i];
        var childName = child.name.split(":")[0];
        if (child.id !== this.node.id && childName === name) {
          return "Duplicated key: " + name;
        }
      }
    }
    return true;
  };

  NodeEditor.prototype._validateValue = function(v) {
    var t = this.typeElem.val();
    if (["String", "Array", "Map"].indexOf(t) !== -1) return true;

    if (t === "ByteArray") {
      if (v.length % 2 !== 0) return "Deformed: invalid length, must be even";
      if (!/^[a-fA-F0-9]+$/.test(v)) return "Deformed: contains invalid chars";
    } else if (t === "Address" && v.length !== 34 && v.length !== 40) {
      return "Deformed: invalid length";
    } else if (t === "Integer" && !/^[1-9]\d*$/.test(v)) {
      return "Deformed: invalid number";
    }
    return true;
  };

  NodeEditor.prototype.clear = function(node) {
    if (node === undefined) {
      this.node = null;
      this.nameElem.val("");
      this.typeElem.val("");
      this.valElem.val("");
      this._showBool(false);
    } else if (this.node && node.id === this.node.id) {
      this.clear();
    }
  };

  NodeEditor.prototype.update = function(node) {
    if (node.level === 0) return;

    this.node = node;

    this.nameElem.val(node.name.split(":")[0]);
    this.setNameReadOnly(node.level <= 1);

    if (node.level <= 1) {
      this.nameLabelElem.html("Name:");
    } else if (node.getParentNode().dataType === "Array") {
      this.nameLabelElem.html("Index:");
      this.setNameReadOnly(true);
    } else if (node.getParentNode().dataType === "Map") {
      this.nameLabelElem.html("Key:");
    }

    this._showBool(false);
    if (["Array", "Map"].indexOf(node.dataType) !== -1) {
      this.setValueReadOnly(true);
      this.valElem.val(
        (node.children ? node.children.length : 0) + " children"
      );
    } else if (node.dataType === "Boolean") {
      this._showBool(true);
      this.table.find("[name='bool']").removeAttr("checked");
      this.table
        .find(fmt("[name='bool'][value='{0}']", node.data))
        .prop("checked", true);
    } else {
      this.setValueReadOnly(false);
      this.valElem.val(node.data);
    }
    this.typeElem.val(node.dataType);
    this.editor.refreshDecorates();
  };

  function Editor(anchor, params) {
    this.id = "editor-" + idGenerator();
    this.anchor = $(anchor);
    this.params = params;
    this.nodesData = null;
    this.zTreeElem = null;
    this.zTree = null;

    this.resizerElem = null;
    this.tableElem = null;

    this.addChildBtn = null;
    this.deleteBtn = null;
    this.resizer = null;
    this.nodeEditor = null;

    this.draggingNode = null;
  }

  Editor.prototype._initDom = function() {
    this.anchor.addClass("editor").addClass("noselect");

    var html =
      '<div class="left">' +
      '  <div class="ztree"></div>' +
      "  <i></i>" +
      "</div>" +
      '<div class="right">' +
      "  <table>" +
      "    <thead>" +
      "      <tr>" +
      "        <td><div class='inner'>Field</div></td>" +
      "        <td><div class='inner'>Content</div></td>" +
      "      </tr>" +
      "    </thead>" +
      "    <tbody>" +
      "      <tr>" +
      "        <td><div class='inner name-label'>Name:</div></td>" +
      "        <td><div class='inner'><input name='name' type='text' /></div></td>" +
      "      </tr>" +
      "      <tr>" +
      "        <td><div class='inner type-label'>Type:</div></td>" +
      "        <td><div class='inner'><select class='styled' name='type'></select></div></td>" +
      "      </tr>" +
      "      <tr>" +
      "        <td><div class='inner value-label'>Value:</div></td>" +
      "        <td>" +
      "          <div class='inner'>" +
      "            <input name='value' type='text' />" +
      "            <label><input type='radio' name='bool' checked='checked' value='true' /> <span>True</span></label>" +
      "            <label><input type='radio' name='bool' value='false' /> <span>False</span></label>" +
      "          </div>" +
      "        </td>" +
      "      </tr>" +
      "    </tbody>" +
      "    <tfoot>" +
      "      <tr>" +
      "        <td></td>" +
      "        <td><button class='btn-apply'>Apply</button></td>" +
      "      </tr>" +
      "    </tfoot>" +
      "  </table>" +
      "</div>";
    this.anchor.html(html);

    this.leftElem = this.anchor.find("> .left");
    this.rightElem = this.anchor.find("> .right");
    this.tableElem = this.anchor.find("table");
    this.resizerElem = this.anchor.find("> .left > i");
    this.zTreeElem = this.anchor.find("> .left > .ztree");
    this.zTreeElem.attr("id", this.id);

    this._enableResizer();
    this._enableNodeEditor();
    this.addChildBtn = new AddChildButton(this._onAddBtnClick.bind(this));
    this.deleteBtn = new DeleteButton(this._onDelBtnClick.bind(this));

    this.decorateInputs();
  };

  Editor.prototype._onResizerMove = function(resizer) {
    var w1 = this.leftElem.width();
    w1 += resizer.delta.x;

    var w2 = this.rightElem.width();
    w2 -= resizer.delta.x;

    if (w1 < 190 || w2 < 200) return;

    this.leftElem.width(w1);
    this.rightElem.width(w2);
  };

  Editor.prototype._onResizerStop = function(resizer) {};

  Editor.prototype._enableResizer = function() {
    this.resizer = new DragDrop(this.resizerElem, {
      onMove: this._onResizerMove.bind(this),
      onStop: this._onResizerStop.bind(this)
    });
    this.resizer.init();
    $(window).on("resize", this._updateResizerHeight.bind(this));
  };

  Editor.prototype._enableNodeEditor = function() {
    this.nodeEditor = new NodeEditor(this.tableElem, this);
    this.nodeEditor.init();
  };

  Editor.prototype._onAddBtnClick = function(node) {
    var type = "String";
    var val = "new string";
    var prefix = "";
    if (node.dataType === "Map") {
      prefix = "new key" + idGenerator();
    } else {
      prefix = node.children ? node.children.length : 0;
    }
    var newNode = paramToNode(
      {
        type: "String",
        value: "new string",
        name: prefix + ": " + val,
        title: type + ": " + val
      },
      node.id
    );
    this.zTree.addNodes(node, newNode);
  };

  Editor.prototype._onDelBtnClick = function(node) {
    Ext.MessageBox.confirm(
      "Confirm",
      "Are you sure?",
      function(yesno) {
        if (yesno === "yes") {
          this.zTree.removeNode(node);
          this.nodeEditor.clear();

          // update children index
          if (node.getParentNode().dataType === "Array") {
            var cs = node.getParentNode().children;
            for (var i = 0, len = cs.length; i < len; ++i) {
              var c = cs[i];
              var name = c.name.split(":")[1];
              c.name = i + ": " + name;
              this.zTree.updateNode(c);
            }
          }
        }
      }.bind(this)
    );
    return false;
  };

  Editor.prototype._addHoverDom = function(treeId, treeNode) {
    this.deleteBtn.show(treeNode);
    this.addChildBtn.show(treeNode);
  };

  Editor.prototype._removeHoverDom = function(treeId, treeNode) {
    this.addChildBtn.hide(treeNode);
    this.deleteBtn.hide(treeNode);
  };

  Editor.prototype._onNodeClick = function(evt, id, node) {
    this.nodeEditor.update(node);
  };

  Editor.prototype._beforeDragNode = function(treeId, treeNodes) {
    if (treeNodes.length > 1) return false;

    var node = treeNodes[0];
    this.nodeEditor.clear();
    if (node.getParentNode().dataType !== "Array") return false;

    this.draggingNode = node;
    return true;
  };

  Editor.prototype._onDragNodeInto = function() {
    return false;
  };

  Editor.prototype._onDragNodePrev = function(treeId, nodes, targetNode) {
    return (
      targetNode.getParentNode().id === this.draggingNode.getParentNode().id
    );
  };

  Editor.prototype._onDragNodeNext = function(treeId, nodes, targetNode) {
    return (
      targetNode.getParentNode().id === this.draggingNode.getParentNode().id
    );
  };

  Editor.prototype._onDragNodeStop = function(
    event,
    treeId,
    treeNodes,
    targetNode,
    moveType
  ) {
    if (moveType === null) return;

    var nodes = this.draggingNode.getParentNode().children;
    for (var i = 0, len = nodes.length; i < len; ++i) {
      var node = nodes[i];
      var p1 = node.name.split(":")[1];
      node.name = i + ": " + p1;
      this.zTree.updateNode(node);
    }
  };

  var updateResizerHeightThrottle = 0;
  Editor.prototype._updateResizerHeight = function() {
    clearTimeout(updateResizerHeightThrottle);
    updateResizerHeightThrottle = setTimeout(
      function() {
        var h = this.leftElem.height();
        var h1 = this.leftElem.prop("scrollHeight");
        this.resizerElem.height(h > h1 ? h : h1);
      }.bind(this),
      100
    );
  };

  Editor.prototype._onNodeCreated = function() {
    this._updateResizerHeight();
  };

  Editor.prototype._delegatesRadioIconClick = function() {
    $(".formelements_check+span").click();
  };

  // the plugin `formelement` has a bug on it's radio's add-on:
  // click the span changes the state of underling radio but click
  // the fake icon does not work, use below code to delegate
  // the click events
  Editor.prototype._fixRadioButton = function() {
    $(".formelements_check").off("click", this._delegatesRadioIconClick);
    $(".formelements_check").on("click", this._delegatesRadioIconClick);
  };

  Editor.prototype.decorateInputs = function() {
    $('input[type="checkbox"], input[type="radio"], select').formelements();
    this._fixRadioButton();
  };

  Editor.prototype.refreshDecorates = function() {
    $('input[type="checkbox"], input[type="radio"], select').formelements(
      "refresh"
    );
    this._fixRadioButton();
  };

  Editor.prototype._beforeNodeClick = function(id, node) {
    return node.level > 0;
  };

  Editor.prototype.getParams = function() {
    return nodeToParam(this.zTree.getNodes()[0]);
  };

  Editor.prototype._initZTree = function() {
    this.zTree = $.fn.zTree.init(
      this.zTreeElem,
      {
        edit: {
          enable: true,
          showRemoveBtn: false,
          showRenameBtn: false,
          drag: {
            prev: this._onDragNodePrev.bind(this),
            next: this._onDragNodeNext.bind(this),
            inner: this._onDragNodeInto.bind(this)
          }
        },
        view: {
          addHoverDom: this._addHoverDom.bind(this),
          removeHoverDom: this._removeHoverDom.bind(this)
        },
        data: {
          key: {
            title: "title"
          }
        },
        callback: {
          onClick: this._onNodeClick.bind(this),
          beforeDrag: this._beforeDragNode.bind(this),
          onDrop: this._onDragNodeStop.bind(this),
          onNodeCreated: this._onNodeCreated.bind(this),
          beforeClick: this._beforeNodeClick.bind(this)
        }
      },
      this.nodesData
    );
  };

  Editor.prototype._hideRightPanelIfNoParameter = function() {
    if (this.nodesData.children.length === 0) {
      this.anchor.find("> .right").hide();
      this.anchor.find("> .left > i").hide();
    }
  };

  Editor.prototype._editFirstParameter = function() {
    if (this.nodesData.children.length === 0) return;

    console.log(this.nodesData.children[0]);
    var id = this.nodesData.children[0].id;
    var nodes = this.zTree.getNodesByParam("id", id);
    if(nodes.length !== 1) return;

    var node = nodes[0];
    this.zTree.selectNode(node);
    this.nodeEditor.update(node);
  };

  Editor.prototype.attach = function() {
    if (this.zTree !== null) return;

    this.nodesData = paramToNode(this.params, -1);
    this._initDom();
    this._initZTree();
    this._hideRightPanelIfNoParameter();
    this._editFirstParameter();
  };

  window.Editor = Editor;
});
