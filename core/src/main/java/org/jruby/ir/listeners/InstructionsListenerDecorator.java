package org.jruby.ir.listeners;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jruby.ir.instructions.Instr;

public class InstructionsListenerDecorator implements List<Instr> {
    private final List<Instr> instrs;
    
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
        return new Iterator<Instr>() {
            private Iterator<Instr> iterator;
            
            { this.iterator = instrs.iterator(); }

            @Override
            public boolean hasNext() {
              return iterator.hasNext();
            }

            @Override
            public Instr next() {
                return iterator.next(); 
            }

            @Override
            public void remove() {
                //TODO enable listening on removing
                iterator.remove();
            }
        };
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
        // TODO call to ListIterator with eventing
        return instrs.listIterator();
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
