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

package org.jruby.parser;

import java.io.PrintStream;

/**
 * Stubbed out version of our own yydebug impl for debugging if we ever find the need.
 */
public class YYDebug {
    private final PrintStream out;

    public YYDebug() {
        out = System.out;
    }

    public void accept(Object value) {
        out.println("accept\tvalue " + value);
    }

    public void discard(int state, int token, String name, Object value) {
        out.println("discard\tstate " + state + "\ttoken(int) " + name + "(" + token + ")\tvalue " + value);
    }

    public void error(String message) {
        out.println("error\t"+message);
    }

    public void lex(int state, int token, String name, Object value) {
        out.println("lex\tstate " + state + "\treading " + name + "(" + token + ")\tvalue " + value);
    }

    public void pop(int state) {
        out.println("pop\tstate " + state + "\ton error");
    }

    public void push(int state, Object value) {
        out.println("push\tstate " + state + "\tvalue " + value);
    }

    public void reduce(int from, int to, int rule, String text, int len) {
        out.println("reduce\tstate " + from + "\tuncover " + to + "\trule (" + rule + ") " + text);
    }

    public void reject() {
        out.println("reject");
    }

    public void shift(int from, int to, int errorFlag) {
        switch (errorFlag) {
            default:                // normally
                out.println("shift\tfrom state " + from + " to " + to);
                break;
            case 0:
            case 1:
            case 2:        // in error recovery
                out.println("shift\tfrom state " + from + " to " + to
                        + "\t" + errorFlag + " left to recover");
                break;
            case 3:                // normally
                out.println("shift\tfrom state " + from + " to " + to + "\ton error");
                break;
        }
    }

    public void shift(int from, int to) {
        out.println("goto\tfrom state " + from + " to " + to);
    }
}
