# -*- coding: utf-8 -*-
import os
os.environ['DJANGO_SETTINGS_MODULE'] = 'settings'
from django.db import models

class User(models.Model):
    name = models.CharField()

    def __unicode__(self):
        return "%s" % self.name
    class Meta:
        db_table = 'users'
        app_label = "flow"


class Message(models.Model):
    timestamp = models.DateTimeField(auto_now_add=False)
    user = models.ForeignKey(User)
    body = models.TextField()

    def __unicode__(self):
        return "%s: ''%s" % (self.user.name, self.body)
    class Meta:
        db_table = 'messages'
        app_label = "flow"
