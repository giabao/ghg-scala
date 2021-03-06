window.React    = require('react');
window.ReactDOM = require('react-dom');

var injectTapEventPlugin = require("react-tap-event-plugin");
//Needed for onTouchTap
//Can go away when react 1.0 release
//Check this repo:
//https://github.com/zilverline/react-tap-event-plugin
injectTapEventPlugin();

window.mui = require("material-ui");
//use new menus
window.mui.Menu = require('material-ui/lib/menus/menu');
window.mui.MenuItem = require('material-ui/lib/menus/menu-item');
window.mui.MenuDivider = require('material-ui/lib/menus/menu-divider');

window.moment = require("moment");
window.d3 = require("d3");
window.ReactFauxDOM = require('react-faux-dom');
window.ReactD3 = require('react-d3-core');
window.ReactD3Shape = require('react-d3-shape');
window.ReactD3Basic = require('react-d3-basic');
