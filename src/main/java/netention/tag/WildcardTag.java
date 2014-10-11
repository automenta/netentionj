/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package netention.tag;

import java.util.regex.Pattern;

/**
 * Specifies a Tag as a wildcard expression allowing '*' for 0 or more wildcard characters,
 * and '?' for 1 wildcard character.
 */
public class WildcardTag extends AbstractTag {
    public final String selector;
    private final Pattern p;

    //ArrayDeque<Object> history;
    public WildcardTag(final String selector) {
        super();
        this.selector = selector;
        p = new WildcardMatcher(selector).pattern;
    }

    public boolean matches(final String topic) {
        return p.matcher(topic).matches();
    }

    public boolean matchesAny(final String... topics) {
        for (final String t : topics) {
            if (p.matcher(t).matches()) {
                return true;
            }
        }
        return false;
    }

    
    
    /*******************************************************************************
     * Copyright (c) 2009, 2014 Mountainminds GmbH & Co. KG and Contributors
     * All rights reserved. This program and the accompanying materials
     * are made available under the terms of the Eclipse Public License v1.0
     * which accompanies this distribution, and is available at
     * http://www.eclipse.org/legal/epl-v10.html
     *
     * Contributors:
     *    Marc R. Hoffmann - initial API and implementation
     *    
     *******************************************************************************/

    /**
     * Matches strings against <code>?</code>/<code>*</code> wildcard expressions.
     * Multiple expressions can be separated with a colon (:). In this case the
     * expression matches if at least one part matches.
     */
    public static class WildcardMatcher {

        private final Pattern pattern;

        /**
         * Creates a new matcher with the given expression.
         * 
         * @param expression
         *            wildcard expressions
         */
        public WildcardMatcher(final String expression) {
            final String[] parts = expression.split("\\:");
            final StringBuilder regex = new StringBuilder(expression.length() * 2);
            boolean next = false;
            for (final String part : parts) {
                if (next) {
                    regex.append('|');
                }
                regex.append('(').append(toRegex(part)).append(')');
                next = true;
            }
            pattern = Pattern.compile(regex.toString());
        }

        private static CharSequence toRegex(final String expression) {
            final StringBuilder regex = new StringBuilder(expression.length() * 2);
            for (final char c : expression.toCharArray()) {
                switch (c) {
                case '?':
                    regex.append(".?");
                    break;
                case '*':
                    regex.append(".*");
                    break;
                default:
                    regex.append(Pattern.quote(String.valueOf(c)));
                    break;
                }
            }
            return regex;
        }

        /**
         * Matches the given string against the expressions of this matcher.
         * 
         * @param s
         *            string to test
         * @return <code>true</code>, if the expression matches
         */
        public boolean matches(final String s) {
            return pattern.matcher(s).matches();
        }

    }
    
}
