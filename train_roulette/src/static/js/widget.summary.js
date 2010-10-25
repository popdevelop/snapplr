(function(owner) {
    if(!owner || !owner.plugins) {
        throw "Could not register widget";
    }
    var p = new Plugin({
        id:   "summary",
        title: "Info",
        init: function() {
            this.$view.append(
                $("<h3>")
                    .text("Twitter hashtags")
                    .click(function() { p.$twitter.slideToggle() })
            );
            this.$twitter = $("<ul>").appendTo(this.$view);

            this.$view.append(
                $("<h3>")
                    .text("Tåg")
                    .click(function() { p.$trains.slideToggle() })
            );
            this.$trains  = $("<ul>").appendTo(this.$view);

            this.$view.append(
                $("<h3>")
                    .text("Användare")
                    .click(function() { p.$users.slideToggle() })
            );
            this.$users  = $("<ul>").appendTo(this.$view);

            Server.send("twitter", { success: function(r) {
                if(r === null) { return; }
                $.each(r, function(i, el) {
                    p.$twitter.append($("<li>").text(el.word));
                });
            }});

            Server.send("trains", { success: function(r) {
                if(r === null) { return; }
                $.each(r, function(i, el) {
                    var srv = "localhost:11000";
                    p.$trains.append($("<li>").html("<a href=\"http://"+ srv +"/"+ el.hash + "\">" + el.name + "</a"));
                });
            }});

            Server.send("users", { success: p.setUsers });
            Server.listen(p.setUsers, TYPE_USER);

        },
        setUsers: function(r) {
            console.log("New users recieved");
            if(r === null) { return; }
            p.$users.empty();
            $.each(r, function(i, el) {
                p.$users.append($("<li>").text(el.name));
            });
        } 
    });
    owner.plugins.push(p);
})(window.Sidebar);
