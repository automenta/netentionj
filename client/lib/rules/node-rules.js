//(function() {
    'use strict';

    //var _ = require('lodash');

    exports.version = '2.2.1';

    function RuleEngine(rules) {

        this.rules = rules;
        this.rules.sort(function(a, b) {
            return b.priority - a.priority;
        });
        return this;
    }

    RuleEngine.prototype.execute = function(fact, callback) {

        //these new attributes have to be in both last session and current session to support
        // the compare function
        fact.process = false;
        fact.result = true;

        var session = _.clone(fact);
        var lastSession = _.clone(fact);
        var _rules = this.rules;

        (function doit(x) {

            if (x < _rules.length && session.process === false) {

                var outcome = true;
                var _rulelist = _.flatten([_rules[x].condition]);

                (function looprules(y) {
                    if(y < _rulelist.length) {
                        _rulelist[y].call({}, session, function(out) {
                            outcome = outcome && out;
                            process.nextTick(function(){
                                return looprules(y+1);
                            });
                        });

                    } else {
                        if (outcome) {
                            var _consequencelist = _.flatten([_rules[x].consequence]);
                            (function loopconsequence(z) {
                                if(z < _consequencelist.length) {
                                    _consequencelist[z].apply(session, [function() {

                                        if (!_.isEqual(lastSession,session)) {
                                            lastSession = _.clone(session);
                                            process.nextTick(function(){
                                                return doit(0);
                                            });
                                        } else {
                                            process.nextTick(function(){
                                                return loopconsequence(z+1);
                                            });
                                        }
                                    }]);
                                } else {
                                    process.nextTick(function(){
                                        return doit(x+1);
                                    });
                                }
                            })(0);
                        } else {
                            process.nextTick(function(){
                                return doit(x+1);
                            });
                        }
                    }
                })(0);
            } else {
                process.nextTick(function(){
                    return callback(session);
                });
            }
        })(0);
    };
  //  module.exports = RuleEngine;
//}(module.exports));
