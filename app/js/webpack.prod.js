const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const webpack = require("webpack");
const baseConfig = require('./webpack.base.js');

module.exports = Object.assign(
  {},
  baseConfig,
  {
    plugins: [
      new UglifyJsPlugin(),
      new webpack.DefinePlugin({
        "process.env": {
          "NODE_ENV": '"production"'
        }
      }),
    ],
  }
);
