import jpype
import time

t = time.time()
classpath = "-Djava.class.path=/Users/johangyllenspetz/.m2/repository/org/snapplr/spatial/0.0.1-SNAPSHOT/spatial-0.0.1-SNAPSHOT.jar" #java.class.path=praat.jar"
jpype.startJVM(jpype.getDefaultJVMPath(),"-ea",classpath)
print time.time() - t

t = time.time()
geosnappr=jpype.JPackage("com.geosnappr")
print geosnappr.Geosnappr.getClosestEdges()
print time.time() - t

jpype.shutdownJVM()

