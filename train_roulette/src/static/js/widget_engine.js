/*
 * Simple functions for a view
 */
function View(obj) {
    $.extend(this, obj);
    this.hideMe();
}
View.prototype.showMe = function() {
    this.show();
    this.removeClass('hidden');
};
View.prototype.hideMe = function() {
    this.hide();
    this.addClass('hidden');
};



/*
 * Define the plugin class
 */

function Plugin(obj) {
    $.extend(this, obj);
}

Plugin.prototype.$view = false;
Plugin.prototype.id    = "default";
Plugin.prototype.title = "Default Name";
/* Called at plugin load */
Plugin.prototype.init  = function() {
    console.log("Hello my name is " + this.name);
};

/* Called when revealing plugin */
Plugin.prototype.focus = $.noop;
Plugin.prototype.blur  = $.noop;

/*
 * Sidebar framework
 */

(function (window) {
    var plugins = [],
        $view = false,
        $tmpl_wgt = false,
        $tmpl_tab = false,
        $tabs = false;
    
    var init = function(view) {
        // Load view for sidebar
        $view = $(view);
        $tabs = $("#tabs");
        // Check if we have templates for the sidebar view
        $tmpl_wgt = $("#widgetContent");
        $tmpl_tab = $("#widgetTab");

        if(!$view) { throw "Could now find view canvas"; }
        if(!$tmpl_wgt || !$tmpl_tab) { throw "All widget templates not defined"; }
        
        // Load all plugins
        for(var i in plugins) {
            if(plugins.hasOwnProperty(i)) {
                var p = plugins[i];
                // Assign a view area and a tab to the plugin
                p.$view = new View($tmpl_wgt.tmpl(p));
                p.$tab  = $tmpl_tab.tmpl(p);
                
                //
                $view.append(p.$view);
                $tabs.append(p.$tab);

                // Init plugin
                p.init();
            }
        }
        plugins[0].$view.showMe();
        plugins[0].$tab.addClass('active');

        // Make tabs clickable
        $tabs.delegate("*", "click", function(e) {
            var p = $.tmplItem(e.target).data;
            for (var i in plugins) {
                if(plugins.hasOwnProperty(i)) {
                    plugins[i].blur();
                    plugins[i].$view.hideMe();
                    plugins[i].$tab.removeClass('active');
                }
            }
            p.focus();
            p.$view.showMe();
            p.$tab.addClass('active');
        });
    };


    window.Sidebar = {    
        init: init,
        plugins: plugins
    };
})(window);
