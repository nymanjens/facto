/* exported assert, stripPrefix */
'use strict';

// source: http://stackoverflow.com/a/2648463/1218058
String.prototype.endsWith = function (suffix) {
    return (this.substr(this.length - suffix.length) === suffix);
};
String.prototype.startsWith = function(prefix) {
    return (this.substr(0, prefix.length) === prefix);
};

// source: http://stackoverflow.com/a/1978419/1218058
String.prototype.contains = function(it) { return this.indexOf(it) !== -1; };

// source: http://stackoverflow.com/a/17606289/1218058
String.prototype.replaceAll = function(search, replacement) {
    var target = this;
    return target.split(search).join(replacement);
};

// source: http://stackoverflow.com/a/1961068/1218058
Array.prototype.unique = function(){
   var u = {}, a = [];
   for(var i = 0, l = this.length; i < l; ++i){
      if(u.hasOwnProperty(this[i])) {
         continue;
      }
      a.push(this[i]);
      u[this[i]] = 1;
   }
   return a;
}

// source: http://stackoverflow.com/a/15313435/1218058
function assert(condition, message) {
    if (!condition) {
        message = message || "Assertion failed";
        if (typeof Error !== "undefined") {
            throw new Error(message);
        }
        throw message; // Fallback
    }
}

function stripPrefix(string, prefix) {
    assert(string.startsWith(prefix));
    return string.substring(prefix.length);
}
