const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const webpack = require("webpack");
const scalajsConfig = require('./scalajs.webpack.config');

module.exports = Object.assign(
  {},
  scalajsConfig,
  {
    node: {
      fs: "empty",
    },
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
