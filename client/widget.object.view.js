//Views for individual objects

var objectView = { };

objectView.chat = {
    start: function(x, options) {
        return newChannelWidget(x.id);
    },
    stop: function() { }
};

objectView.info = {
    start: function(x, options) {
        
        var depthRemaining = options.depthRemaining || 1;
        var depth = options.depth || depthRemaining;
        var nameClickable = (options.nameClickable !== undefined) ? options.nameClickable : true;
        var showName = (options.showName !== undefined) ? options.showName : true;
        var showAuthorIcon = (options.showAuthorIcon !== undefined) ? options.showAuthorIcon : true;
        var showAuthorName = (options.showAuthorName !== undefined) ? options.showAuthorName : true;
        var hideAuthorNameAndIconIfZeroDepth = (options.hideAuthorNameAndIconIfZeroDepth !== undefined) ? options.hideAuthorNameAndIconIfZeroDepth : false;
        var showMetadataLine = (options.showMetadataLine !== undefined) ? options.showMetadataLine : true;
        var showReplyButton = (options.showReplyButton !== undefined) ? options.showReplyButton : true;
        var titleClickMode = (options.titleClickMode !== undefined) ? options.titleClickMode : 'view';
        var showTime = (options.showTime !== undefined) ? options.showTime : true;
        var replyCallback = options.replyCallback ? function(rx) { options.replyCallback(rx); } : null;
        var startMinimized = (options.startMinimized != undefined) ? options.startMinimized : false;
        
        var xid = x.id;
        
        var d = newDiv();
        
        
        var xn = x.name || '';
        if (x._class)
            xn += ' (tag)';
        if (x._property)
            xn += ' (property)';

        var authorID = x.author;

        //d.append(cd);


            //showAuthorName = showAuthorIcon = false;

        var replies;

        if (showAuthorIcon) {
            if (!((depth === depthRemaining) && (hideAuthorNameAndIconIfZeroDepth))) {

                var authorClient = $N.getObject(authorID);
                if (authorClient) {
                    if (authorID) {
                        newAvatarImage(authorClient).appendTo(d);
                    }
                }
            }
        }



        function minimize() {
            d.addClass('ObjectViewMinimized');
            d.removeClass('ObjectViewMaximized');
        }

        function maximize() {
            d.removeClass('ObjectViewMinimized');
            d.addClass('ObjectViewMaximized');
            ensureMaximized();
        }

        function toggleMaxMin() {
            if (d.hasClass('ObjectViewMinimized'))
                maximize();
            else
                minimize();
            reflowView();
            return false;
        }


        //Name
        if (showName) {

            var haxn = newEle('div').addClass('TitleLine').appendTo(d);
            if (startMinimized)
                haxn.click(toggleMaxMin);

            var xxn = xn.length > 0 ? xn : '?';
            var xauthor = x.author;

            if (!nameClickable) {
                haxn.append(xxn);
            } else {
                haxn.append(newEle('a').html(xxn).click(function(e) {
                    if ((xauthor === $N.id()) && (titleClickMode === 'edit'))
                        newPopupObjectEdit(xid, true);
                    else if (typeof (titleClickMode) === 'function') {
                        titleClickMode(xid);
                    } else {

                        newPopupObjectView(xid, e);
                    }
                    return false;
                }));
            }

            if (showAuthorName && (!((depth === depthRemaining) && (hideAuthorNameAndIconIfZeroDepth)))) {
                if (!isSelfObject(x.id)) { //exclude self objects
                    if (x.author) {
                        var a = x.author;
                        var ai = $N.instance[a];
                        var an = ai ? ai.name || a : a;

                        if (!nameClickable) {
                            haxn.prepend(an, ': ');
                        } else {
                            haxn.prepend(newEle('a').html(an).click(function() {
                                newPopupObjectView(a, true);
                                return false;
                            }), ':&nbsp;');
                        }
                    }
                }
            }
        }

        var maximized = false;

        function ensureMaximized() {
            if (maximized)
                return;

            maximized = true;

            if (replies)
                replies.detach();



            if ((showMetadataLine) && (!x._class) && (!x._property)) {
                var mdl = newMetadataLine(x, showTime).appendTo(d);

                if (showReplyButton && (x.id !== $N.id())) {
                    mdl.append(
                                            ' ',
                                            newEle('a').html('<i class="fa fa-mail-reply"></i>').attr('title', 'Reply').click(function() {
                            newReplyPopup(xid, replyCallback);
                            return false;
                            }),
                                            ' ',
                        newSubjectTagButton('Like', 'fa-thumbs-o-up', subjectTag.Like),
                                            ' ',
                        newSubjectTagButton('Dislike', 'fa-thumbs-o-down', subjectTag.Dislike),
                                            ' ',
                        newSubjectTagButton('Trust', 'fa-check', subjectTag.Trust)
                    );
                }
            }

            //d.append('<h3>Relevance:' + parseInt(r*100.0)   + '%</h3>');


            //var nod = newObjectDetails(x);
            //if (nod)
                //d.append(nod);

            addNewObjectDetails(x, d, showMetadataLine ? ['spacepoint'] : undefined);

            if (replies)
                replies.appendTo(d);

        }

        var r = x.reply;
        if (r) {
            var vr = _.values(r);

            if (vr.length > 0) {
                if (!replies) {
                    replies = newDiv().appendTo(d);
                    if (!hideAuthorNameAndIconIfZeroDepth)
                        replies.addClass('ObjectReply');
                    else
                        replies.addClass('ObjectReplyChatZeroDepth');
                }
                else {
                    replies.empty();
                }
                if (depthRemaining > 0) {
                    var childOptions = _.clone(options);
                    childOptions.depthRemaining = depthRemaining - 1;
                    childOptions.transparent = true;
                    childOptions.hideAuthorNameAndIconIfZeroDepth = false;
                    delete childOptions.scale;

                    //TODO sort the replies by age, oldest first?
                    vr.forEach(function(p) {
                        replies.append(newObjectView(p, childOptions));
                    });
                }
                else {
                    replies.append(vr.length + ' replies...');
                }
            }
        } else {
            if (replies) {
                replies.remove();
                replies = null;
            }
        }
        
        if (startMinimized) {
            minimize();
        }
        else {
            maximize();
        }

        return d;
    },
    stop: function() { }
};

objectView.value = {
    start: function(id, options) {
        var x = newDiv();
        /*
         * 
         *  fa-cc-amex
                fa-cc-discover
                fa-cc-mastercard
                fa-cc-paypal
                fa-cc-stripe
                fa-cc-visa
                fa-credit-card
                fa-google-wallet
                fa-paypal

         */
        return x;
    },
    stop: function() { }
};

objectView.links = {
    start: function(o, options) {
        var id = o.id;
        
        var x = newDiv();
        
        var c = $.getJSON('/object/' + encodeURIComponent(id) + '/activity/json', function(context) {
           x.append(JSON.stringify(context));
        });
        
        var c = $.getJSON('/object/' + encodeURIComponent(id) + '/context/json', function(context) {
            //x.append(JSON.stringify(context));

            var items = [];
            var ctx = context.context;
            for (var k in ctx) {
                items.push({ name: k, value: ctx[k] } );
            }
           
            
            if (items.length > 0) {
                var diameter = 300,
                    format = d3.format(",d"),
                    color = d3.scale.category20c();

                var bubble = d3.layout.pack()
                    .sort(null)
                    .size([diameter, diameter])
                    .padding(1.5);

                var svg = d3.select(x[0]).append("svg")
                    .attr("width", diameter)
                    .attr("height", diameter)
                    .attr("class", "bubble");


                  var node = svg.selectAll(".node")
                      .data(bubble.nodes({children:items}))
                      //.filter(function(d) { return !d.children; })
                    .enter().append("g")
                      .attr("class", "node")
                      .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

                  node.append("title")
                      .text(function(d) { return d.name + ": " + format(d.value); });

                  node.append("circle")
                      .attr("r", function(d) { return d.r; })
                      .style("fill", function(d) { return color(d.name); });

                  node.append("text")
                      .attr("dy", ".3em")
                      .style("text-anchor", "middle")
                      .text(function(d) { if (d.name) return d.name.substring(0, d.r / 3); else return '?'; });
                ;
            }

        });
        
        //    //check for Similarity
        //    var ot = objTags(x);
        //    if ((ot[0] === 'Similar') && (ot[1] === 'similarTo')) {
        //        /*showMetadataLine = false;
        //         showActionPopupButton = false;
        //         showSelectionCheck = false;
        //         showTime = false;
        //         nameClickable = false;*/
        //        return newSimilaritySummary(x);
        //    }

        return x;
    },
    stop: function() { }
};

objectView.empty = {
    start: function(id, options) {
        var x = newDiv();
        return x;
    },
    stop: function() { }
};
