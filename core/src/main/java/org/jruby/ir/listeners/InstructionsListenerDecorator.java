/*
 **** BEGIN LICENSE BLOCK *****
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

import org.jruby.ir.instructions.Instr;

import java.util.*;
import org.jruby.ir.representations.BasicBlock;

public class InstructionsListenerDecorator implements List<Instr> {
    private final BasicBlock basicBlock;
    private final List<Instr> instrs;
    private final InstructionsListener listener;

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

            return index < instrs.size() ? index : instrs.size();
        }

        @Override
        public int previousIndex() {
            int index = currentIndex - 1;

            return index > -1 ? index : -1;
        }

        @Override
        public void remove() {
            listener.instrChanged(basicBlock, currentInstr, null, currentIndex, InstructionsListener.OperationType.REMOVE);
            listIterator.remove();
        }

        @Override
        public void set(Instr e) {
            listener.instrChanged(basicBlock, currentInstr, e, currentIndex, InstructionsListener.OperationType.UPDATE);
            listIterator.set(e);
        }

        @Override
        public void add(Instr e) {
            Instr original = currentIndex + 1 > instrs.size() ? instrs.get(currentIndex +1) : null;
            listener.instrChanged(basicBlock, original, e, currentIndex +1 , InstructionsListener.OperationType.ADD);
            listIterator.add(e);
        }

    }

    public InstructionsListenerDecorator(BasicBlock basicBlock, List<Instr> instrs, InstructionsListener listener) {
        this.basicBlock = basicBlock;
        this.instrs = instrs;
        this.listener = listener;
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
        int index = instrs.size() + 1;
        listener.instrChanged(basicBlock, null, e, index, InstructionsListener.OperationType.ADD);
        return instrs.add(e);
    }

    @Override
    public boolean remove(Object o) {
        int index = instrs.indexOf(o);
        if (index != -1) listener.instrChanged(basicBlock, (Instr) o, null, index, InstructionsListener.OperationType.REMOVE);
        return instrs.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return instrs.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Instr> c) {
        int lastIndex = instrs.size() - 1;
        return addAll(lastIndex, c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Instr> c) {
        ListIterator<Instr> iterator = listIterator(index);
        if (c.isEmpty()) return false;
        for (Instr instr : c) {
            iterator.add(instr);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean hasChanged = false;
        for (Object item : c) {
            boolean hasRemoved = remove(item);
            if (!hasChanged) hasChanged = hasRemoved;
        }
        return hasChanged;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean hasChanged = false;
        ListIterator<Instr> iterator = listIterator();
        while(iterator.hasNext()) {
            int index = iterator.nextIndex();
            Instr instr = iterator.next();
            if (!c.contains(instr)) {
                boolean hasRemoved = remove(instr);
                if(!hasChanged) hasChanged = hasRemoved;
            }
        }
        return hasChanged;
    }

    @Override
    public void clear() {
        retainAll(Collections.emptySet());
    }

    @Override
    public Instr get(int index) {
        return instrs.get(index);
    }

    @Override
    public Instr set(int index, Instr element) {
        Instr oldElement = instrs.get(index);
        listener.instrChanged(basicBlock, oldElement, element, index, InstructionsListener.OperationType.UPDATE);
        return instrs.set(index, element);
    }

    @Override
    public void add(int index, Instr element) {
        listener.instrChanged(basicBlock, null, element, index, InstructionsListener.OperationType.ADD);
        instrs.add(index, element);
    }

    @Override
    public Instr remove(int index) {
        Instr element = instrs.remove(index);
        listener.instrChanged(basicBlock, element, null, index, InstructionsListener.OperationType.REMOVE);
        return element;
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
        InstructionsListIterator iterator = new InstructionsListIterator();
        while (iterator.nextIndex() < index) {
            iterator.next();
        }
        return iterator;
    }

    @Override
    public List<Instr> subList(int fromIndex, int toIndex) {
        return instrs.subList(fromIndex, toIndex);
    }

}
