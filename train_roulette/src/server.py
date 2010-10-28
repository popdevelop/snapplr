#
# -*- coding: utf-8 -*-
# Copyright 2010 Popdevelop
#

from tornado.options import define, options
define("port", default=13000, help="run on the given port", type=int)
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
            (r"/config", ConfigHandler),
            (r"/message/new/(.*)", MessageNewHandler),
            (r"/message/updates/(.*)", MessageUpdatesHandler),
            (r"/pad/write", pad.WriteHandler),
            (r"/pad/updates", pad.UpdatesHandler),
            (r"/rooms", RoomsHandler),
            (r"/geotrains", GeoTrainHandler),
            (r"/admin/(.*)", admin.AdminHandler),
            (r"/users", UsersHandler),
            (r"/(.+)", RoomHandler),
            (r"/", MainHandler),
        ]

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


class RoomsHandler(base.BaseHandler):
    @base.authenticated
    def post(self):
        rooms = [model_to_dict(r) for r in Chat.objects.all()]
        self.write(cjson.encode(rooms))


class GeoTrainHandler(base.BaseHandler):
#    @base.authenticated
    @tornado.web.asynchronous
    def get(self):
        print "fetch trains"

        #ms,km and minutes
        lon = self.get_argument("lon", None)
        lat = self.get_argument("lat", None)
        radius = self.get_argument("radius", None)
        time = self.get_argument("time", None)
        forward = self.get_argument("forward", None)
        server = self.get_argument("server", None)
        self.get_trains(server,lon,lat,radius,time,forward)
        print "got lon:%s lat:%s" % (lon,lat)
        print "got radius:%s time:%s" % (radius,time)
        print "got forward:%s" % (forward)
        print "got serve:%s" % (server)
        print "get trains 3"


    def get_trains(self,server,lon, lat, radius, time, forward):
        print "get trains 2"
        http = tornado.httpclient.AsyncHTTPClient()
        
        url = "http://" + server + "/" + lon + "/" + lat + "/" + radius + "/" + time + "/" + forward
#        url = "http://www.google.com"
        print "URL",url
        http.fetch(url, callback=self.on_response)

    def on_response(self, response):
        print "get trains 2.5"
        if response.error: raise tornado.web.HTTPError(500)
        print response.body
        trains = [{'name':'joel','id':'233'}, {'name':'peter','id':'213'}, {'name':'brissmyr','id':'223'}, {'name':'achaido','id':'243'}, {'name':'jacob','id':'283'}, {'name':'sebastian','id':'211'}, {'name':'brissmyr','id':'211'}]
        st = cjson.decode(response.body)
        trains = []
        print st
        for a in st:
            trains.append({'name':a, 'id':1, 'from':st[a]["from"], 'to':st[a]["to"], 'departure':st[a]["departure"]})
            print a + " is id" 

        print trains
        self.write(cjson.encode(trains))
        self.finish()

        print "SUCCESS"

class RoomHandler(base.BaseHandler):
    @base.authenticated
    def get(self, key):
        chat, created = Chat.objects.get_or_create(\
            key=key, defaults={"name": "New room"})
        logging.debug("Loaded chat (id=%d, key='%s', name='%s') Created: %d" %
                      (chat.id, chat.key, chat.name, created))

        if "iPhone" in self.request.headers['User-Agent']:
            self.render("iphone.html")
        else:
            self.render("index.html",
                        messages=Message.objects.all(), chat_id=chat.id)


class ConfigHandler(base.BaseHandler):
    @base.authenticated
    def get(self):
        self.render("config.html", user=User.objects.all()[0])


class MainHandler(base.BaseHandler):
    @base.authenticated
    def get(self):
        self.render("trains.html")


class MessageNewHandler(base.BaseHandler, base.MessageMixin):
    @base.authenticated
    def post(self, chatid):
        current_user = self.get_current_user()
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
        self.authenticate_redirect(ax_attrs=["name", "email"])

    def _on_auth(self, user):
        if not user:
            raise tornado.web.HTTPError(500, "Google auth failed")
        print user
        self.set_secure_cookie("user", user["email"])
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
    logging.basicConfig(format='%(asctime)s %(levelname)s: %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S',
                        level=logging.DEBUG)

    tornado.options.parse_command_line()

    # Enable Ctrl-C when using threads
    signal.signal(signal.SIGINT, signal.SIG_DFL)

    database.Database()

    print "loglevel=%s" % options.logging

    logging.info ("-- STARTING, logging = %s --", options.logging)

    http_server = tornado.httpserver.HTTPServer(Application())
    http_server.listen(options.port)

    cfg = config.load("popchat.conf")
    if cfg != {}:
        LoadPlugins(cfg['active_plugins'])

    print "FlowPad started at port %d" % options.port


    from admin import add_admin_interface
    add_admin_interface("user", User, ["list", "update"])


    tornado.ioloop.IOLoop.instance().start()

if __name__ == "__main__":
    main()
