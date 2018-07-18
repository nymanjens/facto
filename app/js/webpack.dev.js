const scalajsConfig = require('./scalajs.webpack.config');

module.exports = Object.assign(
  {},
  scalajsConfig,
  {
    node: {
      fs: "empty",
    },
  }
);
