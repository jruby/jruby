/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transposed from org.jruby.RubyMatchData,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.jruby.truffle.runtime.core;

import org.joni.Region;
import org.jruby.truffle.nodes.RubyGuards;

/**
 * Represents the Ruby {@code MatchData} class.
 */
@Deprecated
public class RubyMatchData extends RubyBasicObject {

    public static class MatchDataFields {
        public final RubyBasicObject source; // Class
        public final RubyBasicObject regexp; // Regexp
        public final Region region;
        public final Object[] values;
        public final RubyBasicObject pre; // String
        public final RubyBasicObject post; // String
        public final RubyBasicObject global; // String
        public boolean charOffsetUpdated;
        public Region charOffsets;
        public final int begin, end;
        public Object fullTuple;

        public MatchDataFields(RubyBasicObject source, RubyBasicObject regexp, Region region, Object[] values, RubyBasicObject pre, RubyBasicObject post, RubyBasicObject global, int begin, int end) {
            this.source = source;
            this.regexp = regexp;
            this.region = region;
            this.values = values;
            this.pre = pre;
            this.post = post;
            this.global = global;
            this.begin = begin;
            this.end = end;
        }
    }

    public final MatchDataFields fields;

    public RubyMatchData(RubyClass rubyClass, RubyBasicObject source, RubyBasicObject regexp, Region region, Object[] values, RubyBasicObject pre, RubyBasicObject post, RubyBasicObject global, int begin, int end) {
        super(rubyClass);
        assert RubyGuards.isRubyRegexp(regexp);
        fields = new MatchDataFields(source, regexp, region, values, pre, post, global, begin, end);
    }

}
