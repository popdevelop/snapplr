
var TRAIN_VIEW = "trainlist";

var DEFAULT_PAGE = "trains";

// Select train view

(function() {
    var $view = false;
    function draw(r, text, obj) {
        $view.empty();
        var t = r.trains || r;
        if(t.length === 0) {
            $view.append($("<li>").text("No trains"));
            return;
        }
        for(var i in t) {
            if(t.hasOwnProperty(i)) {
                $("<li>").
                    data('id', t[i].id).
                    text(t[i].name).
                    appendTo($view);
            }
        }
    }
    function error(r, text, obj) {
        $view.html("No connection");
    }
    function fetch() {
        Server.send("getTrains", {
            success: draw,
            error: error
        });
    }
    $(document).ready(function() {
        $view = $("#"+TRAIN_VIEW);
        if(!$view) {
            throw("Trainlist got no view :(");
        }
        $("#"+TRAIN_VIEW+ " li").live("click", function() {
            var id = $(this).data('id');
            Chat.load(id);
        });
        $(document).bind("trains.page", function() {
            fetch();
        });
    });
})();

// Chat view
var Chat = (function() {
    function load(id) {
        alert(id);
    }
    return {
        load: load
    };
})();


// Basic pagination
var Page = (function(){
    var current = DEFAULT_PAGE;

    function hide() {
        $('.page').hide();
    }
    function set(page) {
        hide();
        current = page;
        $("#"+page).show();
        $(document).trigger(page+".page");
    }
    $(document).ready(function() {
        set(DEFAULT_PAGE);
    });
    return {
        set: set,
        current: current
    };
})();

(function() {
    var server = "",
        updater = false;
    var listeners = { all: []};
    var API = {
        getTrains: "trains",
    };
    var defParams = {
        type: "GET",
        dataType: "json",
        timeout: 10000,
        data: { "_xsfr": getCookie("_xsfr") }
    };

    function pushSuccess(response) {
        for(var i in listeners.all) {
            if(listeners.all.hasOwnProperty(i)) {
                listeners.all[i](response.result);
            }
        }
        if(response.type in listeners) {
            var l = listeners[response.type];
            for(i in l) {
                if(l.hasOwnProperty(i)) {
                    l[i](response.result);
                }
            }
        }
    }
    function pushError(response) {
        console.log("received ERROR");
    }

    function follow(id) {
        if(updater) {
            updater.stop();
        }
        var pushOpts = {
            url: "train"+id+"/updates",
            success: pushSuccess,
            error: pushError
        };
        updater = new LongPoll(pushOpts);
        updater.start();
    }


    function send(cmd, params) {
        if( !(cmd in API) ) {
            throw("[API] Invalid command");
        }
        if( typeof(params) != 'object') {
            throw("[API] Invalid parameters");
        }
        $.extend(true, params, defParams);
        params.url = server + "/" + API[cmd];
        $.ajax(params);
    }
    
    function registerListener(func, type) {
        if(typeof(type) == "undefined") {
            type = "all";
        }
        if(typeof(listeners[type]) == "undefined") {
            listeners[type] = [];
        }
        listeners[type].push(func);
    }
    window.Server = {
        send: send,
        listen: registerListener
    };
})();

var LongPoll = function(opts) {
    var url = opts.url || "/",
        connected = false,
        timer = false,
        temptimer = false,
        errorSleepTime = 2000,
        self = this,
        successCB = opts.success || $.noop,
        errorCB   = opts.error || $.noop,
        cursor    = 1; //XXX: Make configurable name

    function poll() {
        // XXX: Workaround for not knowing when connection is restored
        temptimer = setTimeout(function() {
           $(document).trigger("connected.server");
            connected = true;            
        }, 5000);
        // XXX: Remove hardcoded cookie
        var args = {"_xsrf": getCookie("_xsrf")};
        if(cursor) { args.cursor = cursor; args.limit = 100; }
        $.ajax({url: url, type: "POST", dataType: "text",
                data: $.param(args), success: onSuccess,
                error: onError});
    }

    function onSuccess(response) {
        clearTimeout(temptimer);
        try {
            response = eval("("+response+")");
        } catch (e) {
            onError();
            return;
        }
        if(response.result) {
            cursor = response.result.cursor || null;
        }
        //XXX: TODO: solve
        if(response.result && response.result.messages) {
            response.result.messages.reverse();
        }
        //Restore connection state
        if(!connected) {
            $(document).trigger("connected.server");
            connected = true;
        }
        window.setTimeout(successCB, 1, response);
        errorSleepTime = 2000;
        timer = window.setTimeout(poll, 0);
    }

    function onError(response) {
        //errorSleepTime *= 2;
        $(document).trigger("error.server");
        connected = false;
        clearTimeout(temptimer);

        window.setTimeout(errorCB, 1, response);
        console.log("Poll error; sleeping for", errorSleepTime, "ms");
        timer = window.setTimeout(poll, errorSleepTime);        
    }

    return {
        start: function() {
            window.clearTimeout(timer);
            self.timer = window.setTimeout(poll, 500);
        },
        stop: function() {
            errorSleepTime = 2000;
            window.clearTimeout(timer);
        }
    };
};

function getCookie(name) {
    var r = document.cookie.match("\\b" + name + "=([^;]*)\\b");
    return r ? r[1] : undefined;
}