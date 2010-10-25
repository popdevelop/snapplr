import sys
import traceback
def formatExceptionInfo(maxTBlevel=5):
      cla, exc, trbk = sys.exc_info()
      excName = cla.__name__
      try:
          excArgs = exc.__dict__["args"]
      except KeyError:
          excArgs = "<no args>"
      excTb = traceback.format_tb(trbk, maxTBlevel)
      return (excName, excArgs, excTb)
