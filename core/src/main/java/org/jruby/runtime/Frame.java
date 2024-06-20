/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Frame holds per-call information that needs to persist outside the
 * execution of a given method. Currently a frame holds the following:
 * <ul>
 * <li>The class against which this method is being invoked. This is usually
 * (always?) the class of "self" within this call.</li>
 * <li>The current "self" for the call.</li>
 * <li>The name of the method being invoked during this frame, used for
 * backtraces and "super" invocations.</li>
 * <li>The block passed to this invocation. If the given code body can't
 * accept a block, it will be Block.NULL_BLOCK.</li>
 * <li>Whether this is the frame used for a binding-related call like eval. This
 * is used to determine where to terminate evaled code's backtrace.</li>
 * <li>The current visibility for methods defined during this call. Starts out
 * as PUBLIC by default (in most cases) and can be modified by appropriate
 * Kernel.public/private/protected calls.</li>
 * <li>The jump target marker for non-local returns.</li>
 * </ul>
 * Frames are allocated for all Ruby methods (in compatibility mode, default)
 * and for some core methods. In general, a frame is required for a method to
 * show up in a backtrace, and so some methods only use frame for backtrace
 * information (so-called "backtrace frames").
 *
 * @see ThreadContext
 */
public final class Frame {
    /** The class against which this call is executing. */
    private RubyModule klazz;

    /** The 'self' for this frame. */
    private IRubyObject self;

    /** The name of the method being invoked in this frame. */
    private String name;

    /**
     * The block that was passed in for this frame (as either a block or a &amp;block argument).
     * The frame captures the block for super/zsuper, but also for Proc.new (with no arguments)
     * and also for block_given?.  Both of those methods needs access to the block of the
     * previous frame to work.
     */
    private Block block = Block.NULL_BLOCK;

    /** The current visibility for anything defined under this frame */
    private Visibility visibility = Visibility.PUBLIC;

    /** backref **/
    private IRubyObject backRef;

    /** lastline **/
    private IRubyObject lastLine;

    /** whether this frame has been captured into a binding **/
    boolean captured;

    /** the ID object for the thread that created this frame **/
    final Object threadID;

    /**
     * Empty constructor, since Frame objects are pre-allocated and updated
     * when needed.
     */
    public Frame(Object threadID) {
        this.threadID = threadID;
    }

    /**
     * Used only by static init to avoid accessing NULL_BLOCK before initialized.
     * @param nullBlock
     */
    private Frame(Object threadID, Block nullBlock) {
        this.threadID = threadID;
        this.block = nullBlock;
    }

    /**
     * Copy constructor, since Frame objects are pre-allocated and updated
     * when needed.
     */
    private Frame(Frame frame) {
        assert frame.block != null;

        this.threadID = frame.threadID;
        this.self = frame.self;
        this.name = frame.name;
        this.klazz = frame.klazz;
        this.block = frame.block;
        this.visibility = frame.visibility;
    }

    /**
     * Update the frame with just filename and line, used for top-level frames
     * and method.
     */
    public void updateFrame() {
        updateFrame(null, null, null, Block.NULL_BLOCK);
    }

    /**
     * Update the frame with caller information and method name, so it will
     * show up correctly in call stacks.
     *
     * @param name The name of the method being called
     */
    public void updateFrame(String name) {
        this.name = name;
    }

    /**
     * Update the frame based on information from another frame. Used for
     * cloning frames (for blocks, usually) and when entering class bodies.
     *
     * @param frame The frame whose data to duplicate in this frame
     */
    public void updateFrame(Frame frame) {
        Block block = frame.block;

        block.getClass(); // null check

        this.self = frame.self;
        this.name = frame.name;
        this.klazz = frame.klazz;
        this.block = frame.block;
        this.visibility = frame.visibility;
    }

    /**
     * Update the frame based on the given values.
     *
     * @param klazz The class against which the method is being called
     * @param self The 'self' for the method
     * @param name The name under which the method is being invoked
     * @param block The block passed to the method
     */
    public void updateFrame(RubyModule klazz, IRubyObject self, String name, Block block) {
        block.getClass(); // null check

        this.self = self;
        this.name = name;
        this.klazz = klazz;
        this.block = block;
        this.visibility = Visibility.PUBLIC;
    }

    /**
     * Update the frame based on the given values.
     *
     * @param klazz The class against which the method is being called
     * @param self The 'self' for the method
     * @param name The name under which the method is being invoked
     * @param block The block passed to the method
     */
    public void updateFrame(RubyModule klazz, IRubyObject self, String name, Visibility visibility, Block block) {
        block.getClass(); // null check

        this.self = self;
        this.name = name;
        this.klazz = klazz;
        this.block = block;
        this.visibility = visibility;
    }

    /**
     * Update the frame based on the given values.
     *
     * @param self The 'self' for the method
     */
    public void updateFrameForEval(IRubyObject self) {
        this.self = self;
        this.name = null;
        this.visibility = Visibility.PRIVATE;
    }

    public void updateFrameForBackref() {
        // nothing
    }

    public void clearFrameForBackref() {
        this.backRef = null;
    }

    /**
     * Clear the frame, as when the call completes. Clearing prevents cached
     * frames from holding references after the call is done.
     */
    public Frame clear() {
        this.self = null;
        this.klazz = null;
        this.block = Block.NULL_BLOCK;
        this.backRef = null;
        this.lastLine = null;

        return this;
    }

    /**
     * Clone this frame.
     *
     * @return A new frame with duplicate information to the target frame
     */
    public Frame duplicate() {
        return new Frame(this);
    }

    /**
     * Return class that we are calling against
     *
     * @return The class we are calling against
     */
    public RubyModule getKlazz() {
        return klazz;
    }

    /**
     * Set the class we are calling against.
     *
     * @param klazz the new class
     */
    public void setKlazz(RubyModule klazz) {
        this.klazz = klazz;
    }

    /**
     * Set the method name associated with this frame
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the method name associated with this frame
     *
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the self associated with this frame
     *
     * @return The self for the frame
     */
    public IRubyObject getSelf() {
        return self;
    }

    /**
     * Set the self associated with this frame
     *
     * @param self The new value of self
     */
    public void setSelf(IRubyObject self) {
        this.self = self;
    }

    /**
     * Get the visibility at the time of this frame
     *
     * @return The visibility
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Change the visibility associated with this frame
     *
     * @param visibility The new visibility
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Retrieve the block associated with this frame.
     *
     * @return The block of this frame or NULL_BLOCK if no block given
     */
    public Block getBlock() {
        return block;
    }

    public IRubyObject getBackRef(IRubyObject nil) {
        IRubyObject backRef = this.backRef;
        return backRef == null ? nil : backRef;
    }

    public IRubyObject setBackRef(IRubyObject backRef) {
        return this.backRef = backRef;
    }

    public IRubyObject getLastLine(IRubyObject nil) {
        IRubyObject lastLine = this.lastLine;
        return lastLine == null ? nil : lastLine;
    }

    public IRubyObject setLastLine(IRubyObject lastLine) {
        return this.lastLine = lastLine;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    public Frame capture() {
        captured = true;
        return this;
    }

    public boolean isCaptured() {
        return captured;
    }

    public Object getThreadID() {
        return threadID;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);

        sb.append("Frame<");
        sb.append(klazz);
        if (name != null) sb.append(" in ").append(name);
        sb.append(">");

        return sb.toString();
    }
}
