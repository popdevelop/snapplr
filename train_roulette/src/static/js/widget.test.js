(function(owner) {
    if(!owner || !owner.plugins) {
        throw "Could not register widget";
    }
    var p = new Plugin({
        id:   "scratch",
        title: "Tågkartan",
        init: function() {
            this.timer = false;
            this.$view.append($("<h3>").text("Tågkartan"));
            this.$text = $("<div id=\"maparea\"></div>").appendTo(this.$view);

            this.recievedText = "";
            this.updater = new LongPoll({
                url: "/pad/updates",
                success: this.recieve,
                error: this.error
            });

            var map = new GMap2(document.getElementById("maparea"), 
                          { size: new GSize(400,500)});
            map.setCenter(new GLatLng(59, 13), 7);
            map.setUIToDefault();

            // Get initial pad content
//            Server.send("padUpdates", {
//                success: function() {
                    p.updater.start();
                    // Send new text after typing
                    p.$text.bind("keyup", function(e) {
                        clearTimeout(p.timer);
                        p.timer = setTimeout(p.postChange, 1000);
                    });
 //               }, data:{cursor: 0}});
        },
        focus: function() {
            p.updater.start();
        },
        recieve: function(response) {
            p.recievedText = response.text;
//            p.$text.val(response.text);
        },
        error: function(response) {
            console.log("Maparea update failed");
        },
        postChange: function() {
            if(p.recievedText != p.$text.val()) {
                Server.send("padWrite", {data: {text: p.$text.val()}});
            }
        }
    });
    owner.plugins.push(p);
})(window.Sidebar);
