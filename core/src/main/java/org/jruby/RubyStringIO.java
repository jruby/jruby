/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Ryan Bell <ryan.l.bell@gmail.com>
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2008 Vladimir Sizikov <vsizikov@gmail.com>
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

import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Deprecated shim for what's now in org.jruby.ext.stringio.RubyStringIO
 */
@JRubyClass(name="StringIO")
public abstract class RubyStringIO extends RubyObject {
    protected RubyStringIO(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return org.jruby.ext.stringio.RubyStringIO.open(context, recv, args, block);
    }

    public abstract IRubyObject initialize(IRubyObject[] args, Block unusedBlock);
    @Override
    public abstract IRubyObject initialize_copy(IRubyObject other);
    public abstract IRubyObject append(ThreadContext context, IRubyObject arg);
    public abstract IRubyObject binmode();
    public abstract IRubyObject close();
    public abstract IRubyObject closed_p();
    public abstract IRubyObject close_read();
    public abstract IRubyObject closed_read_p();
    public abstract IRubyObject close_write();
    public abstract IRubyObject closed_write_p();
    public abstract IRubyObject eachInternal(ThreadContext context, IRubyObject[] args, Block block);
    public abstract IRubyObject each(ThreadContext context, IRubyObject[] args, Block block);
    public abstract IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block);
    public abstract IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block);
    public abstract IRubyObject each_byte(ThreadContext context, Block block);
    public abstract IRubyObject each_byte19(ThreadContext context, Block block);
    public abstract IRubyObject bytes(ThreadContext context, Block block);
    public abstract IRubyObject each_charInternal(final ThreadContext context, final Block block);
    public abstract IRubyObject each_char(final ThreadContext context, final Block block);
    public abstract IRubyObject chars(final ThreadContext context, final Block block);
    public abstract IRubyObject eof();
    public abstract IRubyObject fcntl();
    public abstract IRubyObject fileno();
    public abstract IRubyObject flush();
    public abstract IRubyObject fsync();
    public abstract IRubyObject getc();
    public abstract IRubyObject getc19(ThreadContext context);
    public abstract IRubyObject gets(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject gets19(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject getsOnly(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject isatty();
    public abstract IRubyObject length();
    public abstract IRubyObject lineno();
    public abstract IRubyObject set_lineno(IRubyObject arg);
    public abstract IRubyObject path();
    public abstract IRubyObject pid();
    public abstract IRubyObject pos();
    public abstract IRubyObject set_pos(IRubyObject arg);
    public abstract IRubyObject print(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject print19(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject printf(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject putc(IRubyObject obj);
    public static final ByteList NEWLINE = ByteList.create("\n");
    public abstract IRubyObject puts(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject read(IRubyObject[] args);
    public abstract IRubyObject read_nonblock(ThreadContext contet, IRubyObject[] args);
    public abstract IRubyObject readpartial(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject readchar();
    public abstract IRubyObject readchar19(ThreadContext context);
    public abstract IRubyObject readline(ThreadContext context, IRubyObject[] args);
    public abstract IRubyObject readlines(ThreadContext context, IRubyObject[] arg);
    public abstract IRubyObject reopen(IRubyObject[] args);
    public abstract IRubyObject rewind();
    public abstract IRubyObject seek(IRubyObject[] args);
    public abstract IRubyObject set_string(IRubyObject arg);
    public abstract IRubyObject set_sync(IRubyObject args);
    public abstract IRubyObject string();
    public abstract IRubyObject sync();
    public abstract IRubyObject sysread(IRubyObject[] args);
    public abstract IRubyObject truncate(IRubyObject arg);
    public abstract IRubyObject ungetc(IRubyObject arg);
    public abstract IRubyObject ungetc19(ThreadContext context, IRubyObject arg);
    public abstract IRubyObject write(ThreadContext context, IRubyObject arg);
    public abstract IRubyObject set_encoding(ThreadContext context, IRubyObject enc);
    public abstract IRubyObject external_encoding(ThreadContext context);
    public abstract IRubyObject internal_encoding(ThreadContext context);
    @Override
    public void checkFrozen() {
        super.checkFrozen();
    }
}
