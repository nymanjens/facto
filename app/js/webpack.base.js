const scalajsConfig = require('./scalajs.webpack.config');

module.exports = {
  ...scalajsConfig,
  node: {
    fs: "empty",
  },
};
