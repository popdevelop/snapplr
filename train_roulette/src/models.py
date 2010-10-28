# -*- coding: utf-8 -*-
import time
import os
os.environ['DJANGO_SETTINGS_MODULE'] = 'settings'
from django.db import models
from django.forms.models import model_to_dict
import database

class User(models.Model):
    name = models.CharField()
    avatar = models.CharField()
    google = models.CharField()
    github = models.CharField()
    twitter = models.CharField()

    def __unicode__(self):
        return "%s" % self.name
    class Meta:
        db_table = 'users'
        app_label = "flow"


class Chat(models.Model):
    name = models.CharField()
    key = models.CharField()

    def __unicode__(self):
        return "%s" % self.key
    class Meta:
        db_table = 'chats'
        app_label = "flow"


class TwitterWords(models.Model):
    word = models.CharField()

    def __unicode__(self):
        return "%s" % self.word
    class Meta:
        db_table = 'twitterwords'
        app_label = "flow"


class Message(models.Model):
    types = {
        "chat": 0,
        "twitter": 1,
    }

    type = models.IntegerField()
    _timestamp = models.DateTimeField(auto_now_add=True)
    user = models.ForeignKey(User)
    body = models.TextField()
    chat_id = models.CharField()
    # Chat
    # ...

    # GitHub
    hash = models.TextField()

    # Chat
    # ...

    def timestamp(self):
        "Hack to return seconds instead of string"
        return time.mktime(time.strptime(str(self._timestamp), "%Y-%m-%d %H:%M:%S.%f"))
    timestamp = property(timestamp)

    def model_to_dict(self):
        m = model_to_dict(self)
        m["timestamp"] = self.timestamp
        m["name"] = self.user.name
        m["avatar"] = self.user.avatar
#        print m
        return m

    @classmethod
    def create_with_type(self, type, **kwargs):
        return self.objects.create(type=self.types.get(type), **kwargs)

    def __unicode__(self):
        if self.type == 0:
            return "[Chat] %s: '%s'" % (self.user.name, self.body)
        elif self.type == 1:
            return "[Twitter] %s: '%s'" % (self.user.name, self.body)
        else:
            return "[UNKNOWN] %s: '%d'" % (self.user.name, self.type)

    class Meta:
        db_table = 'messages'
        app_label = "flow"


database.Database()
Chat.objects.get_or_create(key="info", name="Tåg Information")

#User.objects.get_or_create(name="Johan Brissmyr", defaults=dict(twitter="brissmyr", github="brissmyr", avatar="http://a2.twimg.com/profile_images/1124914810/image_bigger.jpg", google="brissmyr@gmail.com"))
#User.objects.get_or_create(name="Johan Gyllenspetz", defaults=dict(twitter="gyllen", github="gyllen", avatar="http://a1.twimg.com/profile_images/608078257/johan_bigger.jpg", google="johan.gyllenspetz@gmail.com"))
#User.objects.get_or_create(name="Joel Larsson", defaults=dict(twitter="tilljoel", github="tilljoel", avatar="http://a2.twimg.com/profile_images/540757022/joel_300x300_bigger.png", google="tilljoel@gmail.com"))
#User.objects.get_or_create(name="Peter Neubauer", defaults=dict(twitter="peterneubauer", github="peterneubauer", avatar="http://a1.twimg.com/profile_images/348968437/MyPicture.jpg", google="peterneubauer@gmail.com"))
#User.objects.get_or_create(name="Sebastian Wallin", defaults=dict(twitter="jimtegel", github="wallin", avatar="http://a1.twimg.com/profile_images/1104141977/image_bigger.jpg", google="sebastian.wallin@gmail.com"))
#User.objects.get_or_create(name="Mr Popdevelop", defaults=dict(twitter="popdevelop", github="popdevelop", avatar="http://a2.twimg.com/profile_images/732520034/popdevelop-avatar.png"))

if __name__ == "__main__":
    import database
    database.Database()
    u1 = User.objects.get(twitter="brissmyr")
    u2 = User.objects.get(github="wallin")
    Message.create_with_type("chat", user=u1, body="Hallå, alla!")
    Message.create_with_type("chat", user=u2, body="Tjena grabben. Allt väl?")
    m = Message.objects.all()[0]
    timestamp = m.timestamp
