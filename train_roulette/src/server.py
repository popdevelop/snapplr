# -*- coding: utf-8 -*-
#
# Copyright 2010 Popdevelop
#

from tornado.options import define, options
define("port", default=11000, help="run on the given port", type=int)
define("test", help="Run server in test mode", type=bool)

import admin
import base
import cjson
import database
import logging
import tornado.auth
import tornado.escape
import tornado.httpserver
import os.path
import pad
import threading
import tornado.ioloop
import tornado.web
import uuid
import time
import signal
import test
import config
import random
from plugin import *

from models import *
from django.forms.models import model_to_dict

# Classes
class Application(tornado.web.Application):
    def __init__(self):
        handlers = [
            (r"/auth/login", AuthLoginHandler),
            (r"/auth/logout", AuthLogoutHandler),
            (r"/message/new/(.*)", MessageNewHandler),
            (r"/message/updates/(.*)", MessageUpdatesHandler),
            (r"/pad/write", pad.WriteHandler),
            (r"/pad/updates", pad.UpdatesHandler),
            (r"/trains", TrainHandler),
            (r"/admin/(.*)", admin.AdminHandler),
            (r"/users", UsersHandler),
            (r"/(.*)", MainHandler),
        ]

        if options.test:
            handlers.append((r"/test/database/clean", test.TestDatabaseCleanHandler))
            handlers.append((r"/test/github/fakecheckin", test.TestGithubFakeCheckInHandler))

        cookie = not options.test

        settings = dict(
            cookie_secret="43oETzKXQAGaYdkL5gEmGeJJFuYh7EQnp2XdTP1o/Vo=",
            login_url="/auth/login",
            template_path=os.path.join(os.path.dirname(__file__), "templates"),
            static_path=os.path.join(os.path.dirname(__file__), "static"),
            xsrf_cookies=cookie,
        )
        tornado.web.Application.__init__(self, handlers, **settings)


class UsersHandler(base.BaseHandler):
    @base.authenticated
    def post(self):
        print "fetch users 1 "
        print "self.active_users()", self.active_users()
        users = [model_to_dict(u) for u in self.active_users()]
        print "fetch users 2 "
        print "users:",users
        self.write(cjson.encode(users))

class TrainHandler(base.BaseHandler):
    @base.authenticated
    def post(self):
        print "fetch trains"
        trains = [model_to_dict(u) for u in Chat.objects.all()]
        self.write(cjson.encode(trains))


class MainHandler(base.BaseHandler):
    @base.authenticated
    def get(self,chatid):
        if not chatid:
            created = False
            while not created:
                h = random.getrandbits(32)
                chatid = "%08x" % h
                logging.info("MainHandler redirect to /%s" % chatid)
                chat,created = Chat.objects.get_or_create(hash=chatid)
                print "try to create chat %s, status %s" % (chat,created)
            chat.name = "random %s" % chat.hash
            chat.save()
            self.redirect("/%s" % chatid)
            return

        logging.info("MainHandler chatid=%s" % chatid)

        chat, created = Chat.objects.get_or_create(hash=chatid)
        print "Created:",created
        if created:
            chat.name = "Tåg %s" % chatid
            chat.save()
            logging.info("MainHandler created chat with hash=%s name=%s id=%s" % (chat.hash, chat.name, chat.id))

        logging.info("MainHandler with chat with hash=%s name=%s id=%s" % (chat.hash, chat.name, chat.id))

        if(self.request.headers['User-Agent'].find("iPhone") > 0):
                self.render("iphone.html")
        self.render("index.html", messages=Message.objects.all(), chat_id=chat.id)

class MessageNewHandler(base.BaseHandler, base.MessageMixin):
    @base.authenticated
    def post(self, chatid):
        current_user = self.get_current_user()
        print "new got chatid:",chatid
        chat_id = chatid
        user, created = User.objects.get_or_create(name=current_user)
        message = Message.create_with_type("chat", user=user, chat_id=chat_id,
                                           body=self.get_argument("body"))
        logging.info("MessageNewHandler user=%s body=%s chatid=%s" % (message.user, message.body, message.chat_id))
        self.new_messages([message])


class MessageUpdatesHandler(base.BaseHandler, base.MessageMixin):
    @base.authenticated
    @tornado.web.asynchronous
    def post(self, chatid):
        print "update got chatid:",chatid
        cursor = self.get_argument("cursor", None)
        limit = self.get_argument("limit", None)
        self.wait_for_messages(self.async_callback(self.on_new_messages),
                               cursor=cursor, limit=limit, chat_id=chatid)

    def on_new_messages(self, d):
        # Closed client connection
        if self.request.connection.stream.closed():
            return
        type = d["type"]
        if type == 0:
            messages = d["result"]
            messages = [m.model_to_dict() for m in messages]
            #print "sending message with len %s, cursor=%d" % (len(messages),messages[0]["id"])
            self.finish(cjson.encode(dict(type=type, result=dict(messages=messages, cursor=messages[0]["id"]))))
        elif type == 1:
            users = d["result"]
            users = [model_to_dict(u) for u in users]
            self.finish(cjson.encode(dict(type=type, result=dict(users=users))))
        else:
            logging.error("Unknown type")


class AuthLoginHandler(base.BaseHandler, tornado.auth.GoogleMixin):
    @tornado.web.asynchronous
    def get(self):
        if self.get_argument("openid.mode", None):
            self.get_authenticated_user(self.async_callback(self._on_auth))
            return
        self.authenticate_redirect(ax_attrs=["name"])

    def _on_auth(self, user):
        if not user:
            raise tornado.web.HTTPError(500, "Google auth failed")
        self.set_secure_cookie("user", user["name"])
        self.redirect("/")


class AuthLogoutHandler(base.BaseHandler):
    def get(self):
        self.clear_cookie("user")
        self.write("You are now logged out")

def LoadPlugins(active_plugins):
    logging.info("Load plugins: %s" % active_plugins)
    plugin_dir = "%s/plugins" % os.getcwd()
    plugin_list = get_plugins(plugin_dir)
    init_plugin_system({'plugin_path': plugin_dir, 'plugins': plugin_list})
    plugins = find_plugins()
    pluggo = {}
    for plugin in plugins:
        if not pluggo.has_key(plugin.__name__) and not options.test and plugin.__name__ in active_plugins:
            p = plugin()
            logging.info("Enabling plugin: %s version: %s by: %s" % (plugin.__name__, p.version(), p.author()))
            p.start()
        pluggo[plugin.__name__] = 1

def main():
    print "                                "
    print "    Popdev environment 2010     "
    print "                                "

    logging.basicConfig(format='%(asctime)s %(levelname)s: %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S',filename="popchat.log")

    tornado.options.parse_command_line()

    # Enable Ctrl-C when using threads
    signal.signal(signal.SIGINT, signal.SIG_DFL)

    database.Database()

    print "    loglevel=%s                 " % options.logging

    logging.info ("-- STARTING, logging = %s --", options.logging)

    http_server = tornado.httpserver.HTTPServer(Application())
    http_server.listen(options.port)

    cfg = config.load("popchat.conf")
    if cfg != {}:
        LoadPlugins(cfg['active_plugins'])

    logging.info ("Starting server")


    tornado.ioloop.IOLoop.instance().start()

if __name__ == "__main__":
    main()
