import sqlite3

DB_FILE = "database.db"

class Database():
    def __init__(self):
        self.statements = []
        self.connection = sqlite3.connect(DB_FILE)
        self.cursor = self.connection.cursor()

        self.add_statement("create table if not exists users "
                           "(id INTEGER PRIMARY KEY, name STRING, "
                           "avatar STRING, "
                           "github STRING, twitter STRING)")
        self.add_statement("create table if not exists chats "
                           "(id INTEGER PRIMARY KEY, name STRING, "
                           "key STRING)")
#        self.add_statement("create table if not exists common "
#                           "(id INTEGER PRIMARY KEY, _timestamp INTEGER, "
#                           "user_id INTEGER)")
#        self.add_statement("create table if not exists messages "
#                           "(id INTEGER PRIMARY KEY, common_ptr_id INTEGER, "
#                           "body TEXT, html TEXT)")
#        self.add_statement("create table if not exists commits "
#                           "(id INTEGER PRIMARY KEY, common_ptr_id INTEGER, "
#                           "hash STRING)")
        self.add_statement("create table if not exists messages "
                           "(id INTEGER PRIMARY KEY, _timestamp INTEGER,"
                           "type INTEGER, chat_id INTEGER, user_id INTEGER, body TEXT, "
                           "html TEXT, hash STRING)")
        self.add_statement("create table if not exists twitterwords "
                           "(id INTEGER PRIMARY KEY, word TEXT)")
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

def delete_all_entries():
    db = Database()
#    db.add_statement("delete from users")
    db.add_statement("delete from messages")
    db.add_statement("delete from twitterwords")
    db.add_statement("delete from githubrepos")
    db.commit()


if __name__ == "__main__":
    reset()
