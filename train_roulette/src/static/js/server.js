var TYPE_MESSAGE = 0;
var TYPE_USER    = 1;

function getCookie(name) {
    var r = document.cookie.match("\\b" + name + "=([^;]*)\\b");
    return r ? r[1] : undefined;
}


/*
 * Long poll class
 */

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


/*
 * Server connection
 */

(function() {
    var server = "",
        updater = false;
    var listeners = { all: []};
    var API = {
        postMessage: "message/new/"+chat_id,
        getMessages: "message/updates/"+chat_id,
        gitrepos: "admin/gitrepos",
        twitter: "admin/twitterwords",
        trains: "trains",
        users: "users",
        padUpdates: "pad/updates",
        padWrite: "pad/write"
    };
    var defParams = {
        type: "POST",
        dataType: "json",
        callbackparameter: "callback",
        timeout: 10000,
        data: { "_xsfr": getCookie("_xsfr") }
    };

    function pushSuccess(response) {
        //console.log(response);
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

    function init() {
        var pushOpts = {
            url: "message/updates/"+chat_id,
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
        console.log("registerListener")
        if(typeof(type) == "undefined") {
            console.log("type all")
            type = "all";
        }
        if(typeof(listeners[type]) == "undefined") {
            console.log("undefined")
            listeners[type] = [];
        }
        console.log(listeners)
        listeners[type].push(func);
    }
    window.Server = {
        init: init,
        send: send,
        listen: registerListener
    };
})();
