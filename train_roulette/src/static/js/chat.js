(function() {
    var nbrUnread = 0,
        isActive = true;
    
    function updateTitle(response) {
        var nbrMessages = response.messages.length || 0;
        if (!isActive) {
            nbrUnread += nbrMessages;
            sound.play();
        }
        document.title = "Wavlr";
        if (nbrUnread > 0) {
            document.title = "(" + nbrUnread + ") " + document.title;
        }
    }    
    function init() {
        Server.listen(updateTitle, TYPE_MESSAGE);
    }

    function onBlur() {
        isActive = false;
    }
    function onFocus(){
        isActive = true;
        nbrUnread = 0;
        updateTitle({ messages: [0]});
    }

    if (/*@cc_on!@*/false) { // check for Internet Explorer
        document.onfocusin = onFocus;
        document.onfocusout = onBlur;
    } else {
        window.onfocus = onFocus;
        window.onblur = onBlur;
    }    

    window.FocusHandler = {
        init: init
    };
})();

// Show connection state
(function() {
    var indicator = $("<div>")
        .attr("id", "errorIndicator")
        .text("Reconnecting...")
        .hide();
    $(document).ready(function() {
        indicator.appendTo("body");
    });
    $(document).bind("error.server", function() {
        indicator.show();
    });
    $(document).bind("connected.server", function() {
        indicator.hide();
    });
})();

(function(){
    var $view = $("<div>")
        .attr("id", "msgAlert");
    var $text = $("<p>").appendTo($view);
    var $btn = $("<a>").text("close").click(hide).appendTo($view);
    function hide() {
        $view.hide();
    }
    function isAlert(str) {
        if(str.indexOf("/alert ") === 0) {
            return str.substr(6);
        }
        return false;
    }
    function newAlert(r) {
        var m = r.messages;
        var len = m.length;
        var txt;
        for(var i = len-1; i >= 0; i--) {
            if(m.hasOwnProperty(i) &&
               (txt = isAlert(m[i].body))) {
                $text.empty();
                $text.append($("<h2>").text(m[i].name + " wants you attention:"));
                $text.append(txt);
                $view.show();
                break;
            }
        }
    }
    $(document).ready(function() {
        Server.listen(newAlert, TYPE_MESSAGE);
        $view.hide().appendTo("body");
    });
})();

// Scolling + new messages
ScrollHandler = (function() {
    var mCount = 0,
        newCount = 0,
        $view,
        $elem;

    function newMsg(r) {
        var len = r.messages.length;
        mCount += len;
        newCount += len;
        var s = newCount > 1 ? "s" : "";
        $view.html(newCount + " new message"+s+" below");
    }
    
    function show() {
        $view.fadeIn();
    }
    
    function clear() {
        newCount = 0;
        $view.hide();
    }

    function atBottom() {
        return $elem[0].scrollHeight - $elem.scrollTop() == $elem.outerHeight();
    }
    function goBottom() {
        clear();
        $(document).scrollTo('max', { axis: 'y'});
    }

    $(document).ready(function() {
       $view = $("<div>")
            .attr("id", "newMessageIndicator")
            .hide()
            .click(goBottom)
            .appendTo("body");
        $elem = $("body");
        $(document).scroll(function() { 
            if(atBottom()) {
                clear();
            }
        });
        Server.listen(newMsg, TYPE_MESSAGE);
    });

    return {
        show: show,
        clear: clear,
        goBottom: goBottom,
        atBottom: atBottom
    };
})();

// Main
$(document).ready(function() {

    soundManager.onready(function() {
        sound = soundManager.createSound({
	    id: 'sound',
	    url: ('/static/audio/sound.mp3')
        });
        Sidebar.init("#collab");
        Chat.init();
        FocusHandler.init();
        Server.init();
        //XXX: How to remove for real?
        $("#soundmanager-debug-toggle").remove();
        $("#soundmanager-debug").remove();
    });
});

var MessageQueue = (function() {
    var queue = [];
    function add(m) {
        queue.push(m);
    }
    function clear() {
        for(var i in queue) {
            if(queue.hasOwnProperty(i) &&
               queue[i].isSent()) {
                queue[i].remove();
                delete queue[i];
            }
        }
    }
    return {
        add: add,
        clear: clear
    };
})();

/*
 * Chat/message handler
 */
(function() {
    var opts = { },
        $view = false,
        $newMessage = false,
        self = this;
    function init(newOpts) {
        $.extend(opts, newOpts);
        // XXX: remove hardcode
        $view = $('#messages > ul');
        $newMessage = $("#sendMessage");
        if(!$view) { throw "[chat] View not found!"; }
        
        // Listen for incoming message events
        console.log("got here, before listen")
        Server.listen(handleNewMessages, TYPE_MESSAGE);
        
        $newMessage.bind("submit", function() {
            postMessage();
            return false;
        });
        $newMessage.bind("keyup", function(e) {
            if(e.keyCode === 13) {
                postMessage();
            }
        });
        $("#newMessage").bind("focus", function() {
            ScrollHandler.goBottom();
        });

    }

    function handleNewMessages(response) {
        var m = response.messages;
        MessageQueue.clear();
        var atBottom = ScrollHandler.atBottom();
        console.log("HALLO");
        console.log("M:" + m);

        if(!atBottom){
            ScrollHandler.show();
        }
                        
        if(m.length > 100) {
            m = m.splice(-100,100);
        }
        var newContent =  $("#flowEvent").tmpl(m);
        // Fade in single messages;
        if(m.length === 1) {
            newContent.hide();
            $view.append(newContent);
            newContent.fadeIn();
        } else {
            newContent.appendTo($view);
        }
        //Message grouping
        var $cEl = false;
        var cEl_data = {};
        var $items = $view.find("li");
        var len = $items.length;
        for(var i = 0; i < len; i++) {
            if($cEl !== false) {
                var el = $items[i];
                var data = $.tmplItem(el);
                if((data.data.name === cEl_data.data.name &&
                    cEl_data.data.type === TYPE_MESSAGE) &&
                    data.data.type === TYPE_MESSAGE &&
                   (data.data.timestamp < cEl_data.data.timestamp + 60)) {
                    $(el).remove();
                    $($cEl).find("p")
                        .append("\n")
                        .append(data.data.body)
                        .fadeIn();
                }
                else {
                    $cEl = el;
                    cEl_data = data;
                }
            } 
            else {
                $cEl = $items[i];
                cEl_data = $.tmplItem($cEl);
            }                
        }
        //XXX: I hate this!
        $items = $items.find("div p");
        len = $items.length;
        for(i = 0; i < len; i++) {
            var p = $($items[i]);
            p.html(p.text().decorate());            
        }
        if(atBottom) {
            ScrollHandler.goBottom();
        }
    }

    function postMessage() {
        var text = $("#newMessage").val();
        if(text == '\n') { $("#newMessage").val(''); return false; }

        var m = new Message({text: text});
        MessageQueue.add(m);
        $("#newMessage").val('');
        m.content.appendTo($view);
        m.send();

        ScrollHandler.goBottom();
        return true;
    }
    window.Chat = {
        init: init
    };
})();

function Message(opts) {
    var self = this;
    var text = opts.text || "";
    var content = $("<li>").addClass("pending");
    var sent = false;
    var attempts = 0;

    function send() {
        var errorMsg = $("<p>")
            .text('Message was not sent (try no. ' + (++attempts) + ") ");
        $("<a>")
            .text("Try again")
            .attr("href", "javascript:;")
            .appendTo(errorMsg)
            .click(send);
        Server.send("postMessage", {
            error: function() {
                content.html(errorMsg);
            },
            success: function(r, text, obj){
                if(obj.status === 200) {
                    content.empty();
                    sent = true;
                }
                else {
                    content.html(errorMsg);                    
                }
            },
            data: { body: text }
        });
        content.empty();
        $("<p>")
            .text("Still sending")
            .hide()
            .appendTo(content)
            .delay(1000)
            .fadeIn();
    }
    return {
        send: send,
        remove: function() { content.remove(); },
        isSent: function() { return sent; },
        content: content
    };
}

if(typeof(console) == "undefined") {
    window.console = {
        log: $.noop
    };
}

Number.prototype.sec2time = function() {
    var ts = new Date(this*1000);
    return ts.getHours().zeroPad() + ":" + ts.getMinutes().zeroPad();
};

Number.prototype.zeroPad = function() {
    if(this < 10) {
        return "0"+this;
    }
  return this;
};

String.prototype.nl2br = function() {
    return this.replace(/\n/g,'<br />');
};

String.prototype.decorate = function() {
    return this
    .replace(/<br>/gim, '\n')
    .replace(/(ftp|http|https|file):\/\/([\S]+)(\b|$)/gim, '<a href="$&" target="_blank">$&</a>')
    .replace(/([^\/])(www[\S]+(\b|$))/gim, '$1<a href="http://$2" target="_blank">$2</a>')
    .replace(/\n/gim, "\n<br>");
};

String.prototype.checkAvatar = function() {
    if(this.length < 2) {
        return "static/img/avatar.jpg";
    }
    return this;
}
