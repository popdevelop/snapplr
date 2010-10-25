import logging
import os

def load(filename):
    if not filename:
        logging.error("No filename set")
        raise "NoConfigException"

    defaults = {}
    try:
        execfile(filename, {}, defaults)
    except Exception, e:
        logging.error("Found error in config, %s" % e)

    for d in defaults:
        if d in os.environ:
            logging.info("Overide config with env for: %s = %s" % (d, default[d]))
            defaults[d] = os.environ[d]

    return defaults
