import base
import tornado.web
import database
import cjson
import re

from models import User
from django.forms.models import model_to_dict

admin_interfaces = []

class AdminHandler(base.BaseHandler):
    interface = None

    def list(self):
        ret = []

        if self.get_filter() != None:
            args = self.get_filter()
            for obj in self.type.objects.filter(**args):
                ret.append(model_to_dict(obj))
        else:
            for obj in self.interface['type'].objects.all():
                ret.append(model_to_dict(obj))

        return ret

    def delete(self):
        id = self.get_argument('id')
        self.interface['type'].objects.filter(id=id).delete()

    def add(self):
        # Check all required parameters
        for param in self.interface['required']:
            arg = self.get_argument(param)

        # Add parameters to list
        args = {}
        for param in self.request.arguments:
            if param != "first_name" and param != "last_name" and param != "name":
                args[param] = self.get_argument(param)

        self.interface['type'].objects.create(**args)

    def update(self):
        id = self.get_argument('id')

        # Check all required parameters
        for param in self.interface['requiredupdate']:
            arg = self.get_argument(param)

        # Add parameters to update
        args = {}
        for param in self.request.arguments:
            if param != "first_name" and param != "last_name" and param != "name":
                args[param] = self.get_argument(param)

        obj = self.interface['type'].objects.filter(id=id)

        if obj != None:
            obj = self.interface['type'](**args)
            obj.save()

    def finish_json(self, data):
        json = cjson.encode(data)
        self.set_header("Content-Length", len(json))
        self.set_header("Content-Type", "application/json; charset=utf-8")
        self.write(json)
        self.finish()

    @base.authenticated
    def post(self, bajje):
        global admin_interfaces

        func = re.split("/", self.request.uri)
        name = func[2]

        found_interface = None

        for interface in admin_interfaces:
            if interface['name'] == name:
                found_interface = interface
                func = func[len(func) - 1]
                self.interface = found_interface

        if found_interface == None:
            #FIXME how do we handle error return??
            return

        s = set(self.interface['handlers'])

        if func == "delete" and ("delete" in s):
            data = self.delete()
        elif func == "add" and ("add" in s):
            data = self.add()
        elif func == "update" and ("update" in s):
            data = self.update()
        elif ("list" in s):
            data = self.list()
        else:
            #FIXME how do we handle errors here??
            return

        self.finish_json(data)

    def get_filter(self):
        return None

def add_admin_interface(name, type, handlers, required = [], requiredupdate = []):
    admin_interface = {'name':name, 'type':type, 'handlers':handlers, 'required':required, 'requiredupdate':requiredupdate}
    admin_interfaces.append(admin_interface)
