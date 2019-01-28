(function() {
  var java = (window.java = {});

  var makeFnName = function() {
    return "_js" + new Date().getTime();
  };

  /**
   * thisObj `string`
   * fn `string`
   * params `array`
   * cb `function`
   */
  java.call = function(thisObj, fn, params, cb) {
    if (typeof params === "function") {
      cb = params;
      params = undefined;
    }
    params = params || [];
    if (cb) {
      var fnName = makeFnName();
      window[fnName] = cb;
      params.push(fnName);
    }
    thisObj = window[thisObj];
    fn = thisObj[fn];
    return fn.apply(thisObj, params);
  };
})();
