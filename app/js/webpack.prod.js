const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const baseConfig = require('./webpack.base.js');

module.exports = {
  ...baseConfig,
  plugins: [
    new UglifyJsPlugin()
  ],
};
