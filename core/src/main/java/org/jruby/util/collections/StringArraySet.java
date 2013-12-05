/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2011 Charles O Nutter <headius@headius.com>
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
package org.jruby.util.collections;

import java.util.HashSet;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An RubyArray that maintains an O(1) Set for fast include? operations.
 */
public class StringArraySet extends RubyArray {
    private final Set<String> set = new HashSet<String>();

    public StringArraySet(Ruby runtime) {
        super(runtime, 4);
    }

    @Override
    public synchronized RubyArray append(IRubyObject item) {
        String string = getStringFromItem(item);
        RubyArray result = super.append(item);
        set.add(string);
        return result;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        set.clear();
    }

    @Override
    public synchronized IRubyObject delete(ThreadContext context, IRubyObject item, Block block) {
        String string = getStringFromItem(item);
        IRubyObject result = super.delete(context, item, block);
        set.remove(string);
        return result;
    }

    @Override
    public synchronized IRubyObject delete_if(ThreadContext context, Block block) {
        IRubyObject result = super.delete_if(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized RubyBoolean include_p(ThreadContext context, IRubyObject item) {
        return context.runtime.newBoolean(set.contains(getStringFromItem(item)));
    }

    @Override
    public synchronized IRubyObject replace(IRubyObject orig) {
        IRubyObject result = super.replace(orig);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject unshift(IRubyObject item) {
        String string = getStringFromItem(item);
        IRubyObject result = super.unshift(item);
        set.add(string);
        return result;
    }

    @Override
    public synchronized IRubyObject unshift(IRubyObject[] items) {
        IRubyObject result = super.unshift(items);
        putAll(toJavaArray());
        return result;
    }

    @Override
    public synchronized IRubyObject aset(IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = super.aset(arg0, arg1);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject aset(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject result = super.aset(arg0, arg1, arg2);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject aset19(IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = super.aset19(arg0, arg1);
        rehash();
        return result;
    }

    @Override
    public synchronized RubyArray collectBang(ThreadContext context, Block block) {
        RubyArray result = super.collectBang(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject collect_bang(ThreadContext context, Block block) {
        IRubyObject result = super.collect_bang(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject compact() {
        IRubyObject result = super.compact();
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject drop(ThreadContext context, IRubyObject n) {
        IRubyObject result = super.drop(context, n);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject drop_while(ThreadContext context, Block block) {
        IRubyObject result = super.drop_while(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject flatten_bang(ThreadContext context) {
        IRubyObject result = super.flatten_bang(context);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject flatten_bang(ThreadContext context, IRubyObject arg) {
        IRubyObject result = super.flatten_bang(context, arg);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject flatten_bang19(ThreadContext context) {
        IRubyObject result = super.flatten_bang19(context);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject flatten_bang19(ThreadContext context, IRubyObject arg) {
        IRubyObject result = super.flatten_bang19(context, arg);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert() {
        IRubyObject result = super.insert();
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert(IRubyObject arg) {
        IRubyObject result = super.insert(arg);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert(IRubyObject arg1, IRubyObject arg2) {
        IRubyObject result = super.insert(arg1, arg2);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert(IRubyObject[] args) {
        IRubyObject result = super.insert(args);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert19(IRubyObject arg) {
        IRubyObject result = super.insert19(arg);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert19(IRubyObject arg1, IRubyObject arg2) {
        IRubyObject result = super.insert19(arg1, arg2);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject insert19(IRubyObject[] args) {
        IRubyObject result = super.insert19(args);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject map_bang(ThreadContext context, Block block) {
        IRubyObject result = super.map_bang(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject pop(ThreadContext context) {
        IRubyObject result = super.pop(context);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject pop(ThreadContext context, IRubyObject num) {
        IRubyObject result = super.pop(context, num);
        rehash();
        return result;
    }

    @Override
    public synchronized RubyArray push_m(IRubyObject[] items) {
        RubyArray result = super.push_m(items);
        rehash();
        return result;
    }

    @Override
    public synchronized RubyArray push_m19(IRubyObject[] items) {
        RubyArray result = super.push_m19(items);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject rejectBang(ThreadContext context, Block block) {
        IRubyObject result = super.rejectBang(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject reject_bang(ThreadContext context, Block block) {
        IRubyObject result = super.reject_bang(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject select_bang(ThreadContext context, Block block) {
        IRubyObject result = super.select_bang(context, block);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject shift(ThreadContext context) {
        IRubyObject result = super.shift(context);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject shift(ThreadContext context, IRubyObject num) {
        IRubyObject result = super.shift(context, num);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject slice_bang(IRubyObject arg0) {
        IRubyObject result = super.slice_bang(arg0);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject slice_bang(IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = super.slice_bang(arg0, arg1);
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject unshift() {
        IRubyObject result = super.unshift();
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject unshift19() {
        IRubyObject result = super.unshift19();
        rehash();
        return result;
    }

    @Override
    public synchronized IRubyObject unshift19(IRubyObject item) {
        IRubyObject result = super.unshift19(item);
        rehash();
        return result;
    }

    public synchronized boolean containsString(String element) {
        return set.contains(element);
    }

    private String getStringFromItem(IRubyObject item) {
        return item.convertToString().asJavaString();
    }

    private void rehash() {
        set.clear();
        putAll(toJavaArray());
    }

    private void putAll(IRubyObject[] items) {
        for (IRubyObject item : items) {
            String string = getStringFromItem(item);
            set.add(string);
        }
    }
    
}
