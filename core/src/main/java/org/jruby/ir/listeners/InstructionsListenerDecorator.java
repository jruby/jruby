/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2013 The JRuby team <jruby@jruby.org>
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
package org.jruby.ir.listeners;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jruby.ir.instructions.Instr;

public class InstructionsListenerDecorator implements List<Instr> {
    private final List<Instr> instrs;
    
    private class InstructionsListIterator implements ListIterator<Instr> {
        private Instr currentInstr;
        private int currentIndex;
        private final ListIterator<Instr> listIterator;
        
        public InstructionsListIterator() {
            this.currentInstr = null;
            this.currentIndex = -1;
            this.listIterator = instrs.listIterator();
        }

        @Override
        public boolean hasNext() {
            return listIterator.hasNext();
        }

        @Override
        public Instr next() {
            currentInstr = listIterator.next();
            currentIndex += 1;
            return currentInstr;
        }

        @Override
        public boolean hasPrevious() {
            return listIterator.hasPrevious();
        }

        @Override
        public Instr previous() {
            currentInstr = listIterator.previous();
            currentIndex -= 1;
            return currentInstr;
        }

        @Override
        public int nextIndex() {
            int index = currentIndex + 1;
            if (index < instrs.size()) {
                return index;
            } else {
                return instrs.size();
            }
        }

        @Override
        public int previousIndex() {
            int index = currentIndex - 1;
            if (index > -1) {
                return index;
            } else {
                return -1;
            }
        }

        @Override
        public void remove() {
            // TODO emit event on removal
            listIterator.remove();
        }

        @Override
        public void set(Instr e) {
            // TODO emit update event
            listIterator.set(e);
        }

        @Override
        public void add(Instr e) {
            // TODO emit add event
            listIterator.add(e);
        }
        
    }
    
    public InstructionsListenerDecorator(List<Instr> instrs) {
        this.instrs = instrs;
    }

    @Override
    public int size() {
        return instrs.size();
    }

    @Override
    public boolean isEmpty() {
        return instrs.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return instrs.contains(o);
    }

    @Override
    public Iterator<Instr> iterator() {
        return new InstructionsListIterator();
    }

    @Override
    public Object[] toArray() {
        return instrs.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return instrs.toArray(a);
    }

    @Override
    public boolean add(Instr e) {
        // TODO emit add
        return instrs.add(e);
    }

    @Override
    public boolean remove(Object o) {
        // TODO emit remove event
        return instrs.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return instrs.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Instr> c) {
        // TODO emit a series of adds
        return instrs.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Instr> c) {
        // TODO emit adds
        return instrs.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // TODO emit events on removal instrs
        return instrs.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // TODO emit events on removing of all not in c
        return instrs.retainAll(c);
    }

    @Override
    public void clear() {
        // TODO emit removing all instrs
        instrs.clear();
    }

    @Override
    public Instr get(int index) {
        return instrs.get(index);
    }

    @Override
    public Instr set(int index, Instr element) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void add(int index, Instr element) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Instr remove(int index) {
        // TODO emit remove event
        return instrs.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return instrs.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return instrs.lastIndexOf(o);
    }

    @Override
    public ListIterator<Instr> listIterator() {
        return new InstructionsListIterator();
    }

    @Override
    public ListIterator<Instr> listIterator(int index) {
        // TODO add own implementation for ListIterator
        return instrs.listIterator(index);
    }

    @Override
    public List<Instr> subList(int fromIndex, int toIndex) {
        return instrs.subList(fromIndex, toIndex);
    }
    
}
