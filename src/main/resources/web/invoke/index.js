$(function() {
  var editor;
  window.setupParams = function() {
    // params from java side
    var params = JSON.parse(window._params_);
    editor = new Editor(document.getElementById("app"), params);
    editor.attach();
  };

  $("#btn-invoke").on("click", function() {
    var rootParam = editor.getParams();
    var preExec = $('[name="pre-exec"]').prop("checked");
    rootParam.name = "Parameter";
    invocation.invoke(JSON.stringify(rootParam), preExec);
  });

  $("#btn-debug").on("click", function() {
    var rootParam = editor.getParams();
    rootParam.name = "Parameter";
    invocation.debug(JSON.stringify(rootParam));
  });
});
