import sqlite3

DB_FILE = "database.db"


class Database():
    def __init__(self):
        self.statements = []
        self.connection = sqlite3.connect(DB_FILE)
        self.cursor = self.connection.cursor()

        self.add_statement("create table if not exists users "
                           "(id INTEGER PRIMARY KEY, name STRING)")
        self.add_statement("create table if not exists messages "
                           "(id INTEGER PRIMARY KEY, timestamp INTEGER, "
                           "user_id INTEGER, body TEXT)")
        self.commit()

    def add_statement(self, statement, args = []):
        self.statements.append((statement, args))

    def commit(self):
        map(lambda x: self.cursor.execute(x[0], x[1]), self.statements)
        self.connection.commit()
        self.statements = []


def reset():
    """
    Wipes the database and sets it up again
    """
    import os
    try:
        os.remove(DB_FILE)
    except OSError:
        pass
    Database()


if __name__ == "__main__":
    reset()
