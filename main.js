const electron = require('electron'),
    app = electron.app,  // Module to control application life.
    Menu = electron.Menu,
    BrowserWindow = electron.BrowserWindow;
const template = [
    {
        label: 'View',
        submenu: [
            {
                label: 'Print',
                accelerator: 'CmdOrCtrl+P',
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.print();
                }
            },
            {
                label: 'Reload',
                accelerator: 'CmdOrCtrl+R',
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.reload();
                }
            },
            {
                label: 'Zoom in',
                accelerator: 'CmdOrCtrl+=',
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.webContents.executeJavaScript('webFrame.setZoomLevel(webFrame.getZoomLevel() + 1);');
                }
            },
            {
                label: 'Zoom out',
                accelerator: 'CmdOrCtrl+-',
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.webContents.executeJavaScript('webFrame.setZoomLevel(webFrame.getZoomLevel() - 1);');
                }
            },
            {
                label: 'Reset zoom level',
                accelerator: 'CmdOrCtrl+0',
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.webContents.executeJavaScript('webFrame.setZoomLevel(0);');
                }
            },
            {
                label: 'Toggle Full Screen',
                accelerator: (function() {
                    if (process.platform == 'darwin')
                        return 'Ctrl+Command+F';
                    else
                        return 'F11';
                })(),
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.setFullScreen(!focusedWindow.isFullScreen());
                }
            },
            {
                label: 'Toggle Developer Tools',
                accelerator: (function() {
                    if (process.platform == 'darwin')
                        return 'Alt+Command+I';
                    else
                        return 'Ctrl+Shift+I';
                })(),
                click: function(item, focusedWindow) {
                    if (focusedWindow)
                        focusedWindow.toggleDevTools();
                }
            }
        ]
    },
    {
        label: 'Help',
        role: 'help',
        submenu: [
            {
                label: 'Learn More',
                click: function() { require('electron').shell.openExternal('http://electron.atom.io') }
            }
        ]
    }
];

const menu = Menu.buildFromTemplate(template);
Menu.setApplicationMenu(menu);

// Report crashes to our server.
require('crash-reporter').start();

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is GCed.
var mainWindow = null;

// Quit when all windows are closed.
app.on('window-all-closed', function() {
    // On OS X it is common for applications and their menu bar
    // to stay active until the user quits explicitly with Cmd + Q
    if (process.platform != 'darwin') {
        app.quit();
    }
});

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
app.on('ready', function() {
    mainWindow = new BrowserWindow();
    mainWindow.loadURL('file://' + __dirname + '/index.html');
    mainWindow.maximize();

    // Emitted when the window is closed.
    mainWindow.on('closed', function() {
        // Dereference the window object, usually you would store windows
        // in an array if your app supports multi windows, this is the time
        // when you should delete the corresponding element.
        mainWindow = null;
    });
});
