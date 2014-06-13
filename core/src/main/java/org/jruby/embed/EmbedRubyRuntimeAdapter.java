/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed;

import java.io.InputStream;
import java.io.Reader;
import org.jruby.RubyRuntimeAdapter;

/**
 * Wrapper interface of {@link RubyRuntimeAdapter} for embedding.
 * This interface defines Java friendly parse methods.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public interface EmbedRubyRuntimeAdapter extends RubyRuntimeAdapter {
    /**
     * Parses a script and return an object which can be run(). This allows
     * the script to be parsed once and evaluated many times.
     *
     * @param script is a Ruby script to be parsed
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    EmbedEvalUnit parse(String script, int... lines);

    /**
     * Parses a script given by a reader and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     *
     * @param reader is used to read a script from
     * @param filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    EmbedEvalUnit parse(Reader reader, String filename, int... lines);

    /**
     * Parses a script read from a specified path and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     *
     * @param type is one of the types {@link PathType} defines
     * @param filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    EmbedEvalUnit parse(PathType type, String filename, int... lines);

    /**
     * Parses a script given by a input stream and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     *
     * @param istream is an input stream to get a script from
     * @param filename filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    EmbedEvalUnit parse(InputStream istream, String filename, int... lines);
}
