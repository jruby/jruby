/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Kiel Hodges <jruby-devel@selfsosoft.com>
 * Copyright (C) 2005 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
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
 ***** END LICENSE BLOCK *****/

package org.jruby;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * @deprecated Use {@link org.jruby.main.Main} instead.
 */
@Deprecated(since = "10.0.3.0")
public class Main {
    private final org.jruby.main.Main main;
    public Main(RubyInstanceConfig config) {
        main = new org.jruby.main.Main(config);
    }

    public Main(final InputStream in, final PrintStream out, final PrintStream err) {
        main = new org.jruby.main.Main(in, out, err);
    }

    public Main() {
        main = new org.jruby.main.Main();
    }

    public static void processDotfile() {
        org.jruby.main.Main.processDotfile();
    }

    public static class Status extends org.jruby.main.Main.Status {
        private final org.jruby.main.Main.Status status;

        public Status(org.jruby.main.Main.Status status) {
            this.status = status;
        }

        public boolean isExit() { return status.isExit(); }
        public int getStatus() { return status.getStatus(); }
    }

    /**
     * This is the command-line entry point for JRuby, and should ONLY be used by
     * Java when starting up JRuby from a command-line. Use other mechanisms when
     * embedding JRuby into another application.
     *
     * @param args command-line args, provided by the JVM.
     */
    public static void main(String[] args) {
        org.jruby.main.Main.main(args);
    }

    public Status run(String[] args) {
        return new Status(main.run(args));
    }
}

