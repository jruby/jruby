/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.javasupport.test;

import java.util.ArrayList;

public class VariableArguments {

	public static final String _LEADING_UNDERSCORE = "_";

    public Object[] constants;
    protected final String[] arguments;

    public VariableArguments() {
        constants = arguments = null;
    }

    public VariableArguments(String... args) {
        constants = arguments = args;
    }

    public String[] getArgs() { return arguments; }

    public void setConstants(String constants) {
        this.constants = constants.split(",");
    }

    public void setConstants(String const1, String const2) {
        this.constants = new String[] { const1, const2 };
    }

    public void setConstants(String c, String... constants) {
        this.constants = new String[constants.length + 1];
        this.constants[ constants.length ] = c;
        System.arraycopy(constants, 0, this.constants, 0, constants.length);
    }

    public void setConstants(String... constants) {
        this.constants = constants;
    }

    public static class VarArgOnly extends VariableArguments {

        public VarArgOnly(Object... constants) {
            super( strArgs(constants) );
            this.constants = constants;
        }

        private static String[] strArgs(final Object... constants) {
            ArrayList<String> args = new ArrayList<String>();
            for ( Object constant : constants ) {
                if ( constant instanceof String ) args.add((String) constant);
            }
            return args.toArray( new String[ args.size() ] );
        }

    }

    public static class StringVarArgOnly extends VariableArguments {

        public StringVarArgOnly(String... constants) {
            super(constants);
        }

    }

    public static class SingleArg extends VariableArguments {

        public SingleArg(String constants) {
            super(constants + "_single");
        }

        public SingleArg(String... constants) {
            super(constants);
        }

    }

}
