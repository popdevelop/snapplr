import functools
import logging
import time
import tornado.escape
from tornado.options import options
import tornado.web
from models import *

def authenticated(method):
    @functools.wraps(method)
    def wrapper(self, *args, **kwargs):
        if not options.test:
            auth = tornado.web.authenticated(method)
            return auth(self, *args, **kwargs) #method(self, *args, **kwargs)
        else:
            return method(self, *args, **kwargs)
    return wrapper


class MessageMixin(object):
    waiters = []

    def wait_for_messages(self, callback, cursor=None, limit=None, chat_id=0):
        cls = MessageMixin
        if cursor:
            if limit:
                lim = int(limit)
                print "using limit: %s with cursor %s" % (lim, cursor)
                recent = Message.objects.filter(id__gt=cursor, chat_id__in=[0,chat_id]).order_by('-id')[:lim]
#                print recent.__class__, len(recent), lim
#                for a in recent:
#                    print a.id
            else:
                recent = Message.objects.filter(id__gt=cursor, chat_id__in=[0,chat_id])
            if recent:
                callback(dict(type=0, result=recent))
                return
        cls.waiters.append(callback)

    def new_messages(self, messages):
        cls = MessageMixin
        logging.info("Sending new message to %d listeners" % len(cls.waiters))
        for callback in cls.waiters:
            try:
                callback(dict(type=0, result=messages))
            except:
                logging.error("Error in waiter callback")
        cls.waiters = []

    def new_users(self, users):
        cls = MessageMixin
        logging.info("Sending new users to %d listeners" % len(cls.waiters))
        for callback in cls.waiters:
            try:
                callback(dict(type=1, result=users))
            except:
                logging.error("Error in waiter callback")
        cls.waiters = []


g_active = {}
class BaseHandler(tornado.web.RequestHandler, MessageMixin):
    def prepare(self):
        if self.get_current_user():
            self.update_active_users()

    # Called from parent
    def on_connection_close(self):
        self.update_active_users()

    def get_current_user(self):
        if options.test:
            return self.get_argument("username", "Mr. Test")
        else:
            username = self.get_secure_cookie("user")
            if not username: return None
            return username

    def active_users(self):
        users = User.objects.filter(name__in=g_active.keys())
        return users

    def update_active_users(self):
        now = time.time()
        name = self.get_current_user()
        print "update_active_users with:", name
        global g_active
        print "g_active", g_active
        updated = name not in g_active
        g_active[name] = now
        new_active = {}
        for key in g_active.keys():
            if g_active[key] + 10 > now:
                new_active[key] = g_active[key]
            else:
                updated = True
        g_active = new_active
        print "g_active",g_active
        if updated:
            users = User.objects.filter(name__in=g_active.keys())
#            self.new_users(users)
