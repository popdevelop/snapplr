import neo4j

class Graph:
    class __impl:
        def db(self):
            return self._db

    __instance = None

    def __init__(self):
        if Graph.__instance is None:
            Graph.__instance = Graph.__impl()
            self._db = neo4j.GraphDatabase("snapplr.graph")

        self.__dict__['_Graph__instance'] = Graph.__instance

    def __getattr__(self, attr):
        return getattr(self.__instance, attr)

    def __setattr__(self, attr, value):
        return setattr(self.__instance, attr, value)


if __name__ == '__main__':
    g1 = Graph()
    print g1.db()

    g2 = Graph()
    print g2.db()
