addView({
    id: 'wiki',
    name: 'Wiki',
    icon: 'icon/view.wiki.svg',
    start: function(v) {
        var onWikiTagAdded = function(x) {
            
        };
        
        var wiki = newWikiBrowser(onWikiTagAdded);
        var frame = newDiv().attr('class', 'SelfView');
        frame.append(wiki);

        v.append(frame);

        wiki.onURL = function (u) {
            console.log('uri=' + u);
            uri = u;
        };
        wiki._gotoTag = wiki.gotoTag; //HACK
        wiki.gotoTag = function (page, search) {
            var r;
            if (search) {
                r = 'wiki/search/' + encodeURIComponent(page);
            }
            else {
                r = 'wiki/' + encodeURIComponent(page);
            }
            $N.router.navigate(r, {trigger: false});

            $('#sidebar').empty();


            wiki._gotoTag(page, search);
        };

        frame.onChange = function () {
            //update user summary?
        };
        return frame;
        
    },
    stop: function() {
        
    }
});
