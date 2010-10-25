(function(owner) {
    if(!owner || !owner.plugins) {
        throw "Could not register widget";
    }
    var p = new Plugin({
        id:   "urls",
        title: "Länkar",
        addURL: function(res) {
            var m = res.messages;
            var len = m.length;
            for(var i = 0; i < len; i++) {
                if(m.hasOwnProperty(i) &&
                  (links = m[i].body.getLinks()) !== null) {
                    for(var j in links) {
                        var item = $('<li>')
                            .text(m[i].timestamp.sec2time())
                            .prependTo(p.$urls);
                        $("<span>")
                            .text(" by " + m[i].name)
                            .appendTo(item);
                        $('<a>')
                            .attr("href", links[j])
                            .attr("target", "_new")
                            .text(""+links[j])
                            .appendTo(item);
                    }
                }
            }
            //XXX: Not so effective
            p.$urls.html(p.$urls.find('li').splice(0, 30));
        },
        init: function() {
            this.$view.append($("<h3>").text("Posted URLs"));
            this.$urls = $("<ul>").appendTo(this.$view);
            Server.listen(p.addURL, TYPE_MESSAGE);
        }
    });
    owner.plugins.push(p);
})(window.Sidebar);

String.prototype.getLinks = function() {
  var exp = /(\b(https?):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
  return this.match(exp); 
}
