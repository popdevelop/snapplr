(function(owner) {
    if(!owner || !owner.plugins) {
        throw "Could not register widget";
    }
    var p = new Plugin({
        id:   "flickr",
        title: "Flickr",
        init: function() {
            var self = this;
            $.jsonp({
                url: "http://api.flickr.com/services/feeds/photos_friends.gne",
                data: {
                    user_id: "45839708@N03",
                    format: "json",
                    jsoncallback: "callback"
                },
                callback: "callback",
                success: function(data){
                    $("<h3>").text(data.title).appendTo(self.$view);
                    $.each(data.items, function(i,item){
                        $("<img/>").attr("src", item.media.m).appendTo(self.$view);
                        if ( i == 3 ) return false;
                    });
                }});
        }
    });
    owner.plugins.push(p)
})(window.Sidebar);