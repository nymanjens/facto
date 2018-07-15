var webpack = require("webpack");
var path = require("path");

module.exports = require('./scalajs.webpack.config');

module.exports['node'] = {
   fs: "empty"
};
