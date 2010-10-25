
$(document).ready(function() {
    Chat.init();
    Server.init();
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
        $newMessage = false;
    function init(newOpts) {
        $.extend(opts, newOpts);
        // XXX: remove hardcode
        $view = $('#content > ul');
        $newMessage = $("#sendMessage");
        if(!$view) { throw "[chat] View not found!"; }
        
        // Listen for incoming message events
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

        $view.click(function() {
            $(document).scrollTo('max', { axis: 'y'});
        });
    }
    
    function handleNewMessages(response) {
        var m = response.messages;
        MessageQueue.clear();
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
        $items = $items.find("p");
        len = $items.length;
        for(i = 0; i < len; i++) {
            var p = $($items[i]);
            p.html(p.text().decorate());            
        }
        $(document).scrollTo('max', { axis: 'y'});
    }

    function postMessage() {
        var text = $("#newMessage").val();
        if(text == '\n') { $("#newMessage").val(''); return false; }

        var m = new Message({text: text});
        MessageQueue.add(m);
        $("#newMessage").val('');
        m.content.appendTo($view);
        m.send();

        $(document).scrollTo('max', { axis: 'y'});
        return true;
    }
    window.Chat = {
        init: init
    };
})();

function Message(opts) {
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
    .replace(/(ftp|http|https|file):\/\/[\S]+(\b|$)/gim, '<a href="$&" target="_blank">$&</a>')
    .replace(/([^\/])(www[\S]+(\b|$))/gim, '$1<a href="http://$2" target="_blank">$2</a>')
    .replace(/\n/gim, "\n<br>");
} ;
