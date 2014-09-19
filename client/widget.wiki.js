function getENWikiURL(t) {
    if (t == null)
        return '#';

    if (t[0] == '_')
        return 'http://en.wiktionary.org/wiki/' + t.substring(1).toLowerCase();
    else
        return 'http://en.wikipedia.org/wiki/' + t;
}


function newWikiBrowser(onTagAdded, options) {
    if (!options)
        options = {};

    var b = newDiv();

    var header = newDiv();
    header.addClass('WikiBrowserHeader');
    

    var backButton = $('<button disabled><i class="fa fa-arrow-left"></i></button>');
    var homeButton = $('<button disabled><i class="fa fa-home"></i></button>');
    homeButton.click(function() {
        b.gotoTag(configuration.wikiStartPage);
    });
    
    var searchInput = $('<input _placeholder="Search Wikipedia"/>');
    var searchInputButton = $('<button><i class="fa fa-play"></i></button>');
    searchInput.keyup(function(event) {
        if (event.keyCode == 13)
            searchInputButton.click();
    });
    searchInputButton.click(function() {
        b.gotoTag(searchInput.val(), true);
    });
    header.append(backButton);
    header.append(homeButton);
    header.append('<button disabled title="Bookmark"><i class="fa fa-star"></i></button>');
    header.append('<button disabled><i class="fa fa-refresh"></i></button>');
    header.append(searchInput);
    header.append(searchInputButton);

    b.append(header);

    var br = $('<div/>');
    br.addClass('WikiBrowser');


    function loading() {
        br.html('Loading...');
    }

    var currentTag = configuration.wikiStartPage;

    b.gotoTag = function(t, search) {
        loading();
        currentTag = t;

        var url;
        var extractContent;
        if (configuration.wikiProxy) {
            if (search) {
                var tt = t.replace(/ /g, '_'); //hack, TODO find a proper way of doing this
                url = configuration.wikiProxy + 'en.wikipedia.org/w/index.php?search=' + encodeURIComponent(tt);
            }
            else
                url = configuration.wikiProxy + 'en.wikipedia.org/wiki/' + encodeURIComponent(t);
            extractContent = true;
        }
        else {
            url = search ? '/wiki/search/' + t : '/wiki/' + t + '/html';
            extractContent = false;
        }

        function newPopupButton(target) {
            var p = $('<a href="#" title="Tag">+</a>');
            p.click(function() {
                if (onTagAdded)
                    onTagAdded('dbpedia.org/resource/' + target);
                return false;
            });
            return p;
        }

        $.get(url, function(d) {


            //HACK rewrite <script> tags so they dont take effect
            d = d.replace(/<script(.*)<\/script>/g, '');

            br.empty().append(d);
            var metaele = br.find('#_meta');
            var metadata = JSON.parse(metaele.text());
            metaele.empty();            

            var url = metadata.url;
            currentTag = url.substring(url.lastIndexOf('/')+1, url.length);
            
//            if (extractContent) {
//                //br.find('head').remove();
//                //br.find('script').remove();
//                //br.find('link').remove();
//                if (search) {
//                    //TODO this is a hack of a way to get the acctual page name which may differ from the search term after a redirect happened
//                    var pp = 'ns-subject page-';
//                    var ip = d.indexOf(pp) + pp.length;
//                    var ip2 = d.indexOf(' ', ip);
//                    currentTag = d.substring(ip, ip2);
//                }
//            }
//            else {
//                //WIKIPAGEREDIRECTOR is provided by the node.js server, so don't look for it when using CORS proxy
//                if (search) {
//                    
//                    
//                }
//            }
            


            br.find('a').each(function() {
                var t = $(this);
                var h = t.attr('href');
                t.attr('href', '#');
                if (h) {
                    if (h.indexOf('/wiki') == 0) {
                        var target = h.substring(6);

                        t.click(function() {
                            b.gotoTag(target);
                            return false;
                        });

                        if ((target.indexOf('Portal:') != 0) && (target.indexOf('Special:') != 0)) {
                            t.after(newPopupButton(target));
                        }
                    }
                }
            });
            var lt = newPopupButton(currentTag);

            if (currentTag.indexOf('Portal:') != 0)
                br.find('#firstHeading').append(lt);
        });

    }
    if (options.initialSearch) {
        searchInput.val(options.initialSearch);
        b.gotoTag(options.initialSearch, true);
    }
    else {
        b.gotoTag(currentTag);
    }

    b.append(br);


    return b;
}

function onWikiTagAdded(d, tag) {
    d.empty();
    
    /*var d = newPopup(target, {
        width: 650,
        modal: true
    });*/
    
    
    var tagBar = newTagBar(self, d);
    var saveButton = newTagBarSaveButton(self, tag, tagBar, function () {
        //d.dialog('close');
        d.empty();
    });
    var cancelButton = $('<button title="Cancel" class="cancelButton"><i class="fa fa-times"></i></button>').click(function () {
        //d.dialog('close');
        d.empty();
    });

    d.append( $('<div class="label"></div>').append('<h4><span class="label label-success">' + tag + '</span></h4>') );
    d.append( $('<div class="quicktag"></div>').append(tagBar) );
    d.append( $('<div class="save"></div>').append(saveButton, cancelButton) );
}

function newWikiView(v) {

    var frame = newDiv().attr('class', 'SelfView');
    frame.append(newWikiBrowser(onWikiTagAdded));

    v.append(frame);

    frame.onChange = function () {
        //update user summary?
    };

    return frame;

    /*
     var roster = newRoster();
     roster.addClass('SelfRoster');
     
     var contentTags = newDiv().attr('class', 'SelfViewTags');
     var contentTime = newDiv().attr('class', 'SelfViewTime');
     var content = newDiv().attr('class', 'SelfViewContent');
     
     frame.append(roster);
     frame.append(content);
     
     var currentUser = $N.myself();
     
     function summaryUser(x) {
     currentUser = x;
     content.empty();
     content.append(newSelfSummary(s, x, content));
     content.append(contentTags);       
     content.append(contentTime);       
     updateTags(x);
     }
     
     function updateTags(x) {
     contentTags.html(newSelfTagList(s, x, content));
     
     if (x)
     if (configuration.showPlanOnSelfPage) {
     //contentTime.html(newSelfTimeList(x, contentTime));
     }
     
     roster.html(newRoster(function(x) {
     summaryUser(x);
     }));
     }
     
     summaryUser(currentUser);
     
     v.append(frame);
     
     frame.onChange = function() {
     updateTags(currentUser);
     //update user summary?
     };
     */

}

