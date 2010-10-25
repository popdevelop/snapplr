import re
import os
import sys
import logging
import util

class Plugin(object):
    """
    The base class from which all plugins are derived.  It is used by the
    plugin loading functions to find all the installed plugins.
    """

    def name(self):
        raise "NoNameException"

    def version(self):
        raise "NoVersionException"

    def run(self):
        raise "NoRunException"

    def logger(self):
#        print "return logger with name" % self.name()
        return createLogger(self.name())

def createLogger(name):
     logger=logging.getLogger(name)
     logger.setLevel(logging.DEBUG)
     handler=logging.FileHandler(os.path.join('./plugins/',name+'.log'),'a')
     f = logging.Formatter(fmt=name.upper() + ' %(asctime)s %(levelname)s: %(message)s',datefmt='%Y-%m-%d %H:%M:%S')
     handler.setFormatter(f)
     logger.addHandler(handler)
     return logger

def get_plugins(plugin_dir):
    """Adds plugins to sys.path and returns them as a list"""

    registered_plugins = []

    #check to see if a plugins directory exists and add any found plugins
    # (even if they're zipped)
    if os.path.exists(plugin_dir):
        plugins = os.listdir(plugin_dir)
        pattern = ".py$"
        for plugin in plugins:
            plugin_path = os.path.join(plugin_dir, plugin)
            if os.path.splitext(plugin)[1] == ".zip":
                sys.path.append(plugin_path)
                (plugin, ext) = os.path.splitext(plugin) # Get rid of the .zip extension
                registered_plugins.append(plugin)
            elif plugin != "__init__.py":
                if re.search(pattern, plugin):
                    (shortname, ext) = os.path.splitext(plugin)
                    registered_plugins.append(shortname)
            if os.path.isdir(plugin_path):
                plugins = os.listdir(plugin_path)
                for plugin in plugins:
                    if plugin != "__init__.py":
                        if re.search(pattern, plugin):
                            (shortname, ext) = os.path.splitext(plugin)
                            sys.path.append(plugin_path)
                            registered_plugins.append(shortname)
    return registered_plugins

def init_plugin_system(cfg):
    """
    Initializes the plugin system by appending all plugins into sys.path and
    then using load_plugins() to import them.

        cfg - A dictionary with two keys:
        plugin_path - path to the plugin directory (e.g. 'plugins')
        plugins - List of plugin names to import (e.g. ['foo', 'bar'])
    """
    if not cfg['plugin_path'] in sys.path:
        sys.path.insert(0, cfg['plugin_path'])
    load_plugins(cfg['plugins'])


def find_plugins():
    return Plugin.__subclasses__()

def load_plugins(plugins):
    """
    Imports all plugins given a list.
    Note:  Assumes they're all in sys.path.
    """
    for plugin in plugins:
        __import__(plugin, None, None, [''])
        if plugin not in Plugin.__subclasses__():
            # This takes care of importing zipped plugins:
            __import__(plugin, None, None, [plugin])
