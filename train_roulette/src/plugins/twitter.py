# -*- coding: utf-8 -*-
import sys
import tweetstream
import threading
import base
import logging
from plugin import *
from models import *
import database
import config
from util import *
from admin import add_admin_interface

__VERSION__ = "0.1"
__AUTHOR__ = "popdevelop"
__NAME__ = "twitter"

# Create database entry
db = database.Database()
db.add_statement("create table if not exists twitterwords "
                 "(id INTEGER PRIMARY KEY, word TEXT)")
db.commit()

# Create django model
class TwitterWords(models.Model):
    word = models.CharField()

    def __unicode__(self):
        return "%s" % self.word
    class Meta:
        db_table = 'twitterwords'
        app_label = "flow"

# Populate database

TwitterWords.objects.get_or_create(word="#snapplr")
TwitterWords.objects.get_or_create(word="#sjab")
TwitterWords.objects.get_or_create(word="#sj550")
TwitterWords.objects.get_or_create(word="#sj448")
TwitterWords.objects.get_or_create(word="#sj")

add_admin_interface("twitterwords", TwitterWords, ["list", "update", "add", "delete"], required = ["word"])

class TwitterStream(threading.Thread,  base.MessageMixin, Plugin):
    def __init__(self):
        self.words = []
        self.log = self.logger()
        self.log.debug ("init twitter thread: %s" % str(self.words))
        threading.Thread.__init__(self)
        cfg = config.load("plugins/twitter.conf")
        self.username = cfg['TWITTER_USER']
        self.password = cfg['TWITTER_PASS']

    def name(self):
        return __NAME__

    def version(self):
        return __VERSION__

    def author(self):
        return __AUTHOR__

    def run(self):
        #get sem
        seconds = 300
        while True:
            try:
                self.words = []

                for obj in TwitterWords.objects.all():
                    self.words.append(obj.word)

                self.log.debug ("before streamloop: %s" % self.words)
                with tweetstream.TrackStream(self.username, self.password, self.words) as stream:
                    for tweet in stream:
                        if tweet.has_key('limit'):
                            time.sleep(3)
                            continue
                        if tweet.has_key('delete'):
                            time.sleep(3)
                            continue
                        
                        msg = "%s - %s" % (tweet["user"]["screen_name"], tweet['text'])
                        self.log.debug ("New message: %s" % msg)
                        user, created = User.objects.get_or_create(twitter=tweet["user"]["screen_name"],
                                                                   defaults={"name": tweet["user"]["name"],
                                                                             "avatar": tweet["user"]["profile_image_url"]
                                                                             })
                        message = Message.create_with_type("twitter", user=user, body=tweet["text"])
                        self.new_messages([message])

                        oldwords = self.words
                        for obj in TwitterWords.objects.all():
                            self.words.append(obj.word)
                        if oldwords != self.words:
                            break
            except tweetstream.ConnectionError, e:
                self.log.error ("Disconnected from twitter. Reason: %s" % e.reason)
                time.sleep(seconds)
                pass
            except Exception:
                self.log.error(formatExceptionInfo())
                pass
