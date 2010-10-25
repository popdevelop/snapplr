import base
import tornado.web
import logging
import cjson

text = ""

class PadMixin(object):
    waiters = []

    def wait_for_messages(self, callback, cursor=None):
        cls = PadMixin
        if cursor:
            recent = Message.objects.filter(id__gt=cursor)
            if recent:
                callback(recent)
                return
        cls.waiters.append(callback)

    def new_messages(self, messages):
        cls = PadMixin
        logging.info("Sending new message to %r listeners", len(cls.waiters))
        for callback in cls.waiters:
            try:
                callback(messages)
            except:
                logging.error("Error in waiter callback", exc_info=True)
        cls.waiters = []


class WriteHandler(base.BaseHandler, PadMixin):
    @base.authenticated
    def post(self):
        global text
        text = self.get_argument("text")
        self.new_messages("empty")

class UpdatesHandler(base.BaseHandler, PadMixin):
    @base.authenticated
    @tornado.web.asynchronous
    def post(self):
        self.wait_for_messages(self.async_callback(self.on_new_messages))

    def on_new_messages(self, messages):
        global text
        # Closed client connection
        if self.request.connection.stream.closed():
            return
        self.finish_json({'text':text})

    def finish_json(self, data):
        json = cjson.encode(data)
        self.set_header("Content-Length", len(json))
        self.set_header("Content-Type", "application/json; charset=utf-8")
        self.write(json)
        self.finish()
