addView({
    id: 'wiki',
    name: 'Wiki',
    icon: 'icon/view.wiki.svg',
    start: function(v, param) {
        var onWikiTagAdded = function(tagURI, e) {
            
            if (!$N.object[tagURI]) {
                //create proto-tag
                var newTag = {
                    id: tagURI,
                    name: tagURI,
                    extend: []
                };
                
                $N.add(newTag);
            }
            var v = newPopupObjectView(tagURI, e);
            
            v.append(newWikiTagTagger(tagURI));
        };
        
        var wiki = newWikiBrowser(onWikiTagAdded);
        
		if (!param) {			
        	wiki.gotoTag(configuration.wikiStartPage, false);
		}
		else {
			var search = param.search;
			var target = param.target;
			wiki.gotoTag(target, search);
		}
		
        
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
