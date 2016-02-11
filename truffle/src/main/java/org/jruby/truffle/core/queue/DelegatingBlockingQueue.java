/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.queue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DelegatingBlockingQueue<T> implements BlockingQueue<T> {

    private final BlockingQueue<T> queue;

    public DelegatingBlockingQueue(BlockingQueue<T> queue) {
        this.queue = queue;
    }

    protected BlockingQueue<T> getQueue() {
        return queue;
    }

    @Override
    public boolean add(T t) {
        return queue.add(t);
    }

    @Override
    public boolean offer(T t) {
        return queue.offer(t);
    }

    @Override
    public void put(T t) throws InterruptedException {
        queue.put(t);
    }

    @Override
    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(t, timeout, unit);
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }

    @Override
    public T remove() {
        return queue.remove();
    }

    @Override
    public T poll() {
        return queue.poll();
    }

    @Override
    public T element() {
        return queue.element();
    }

    @Override
    public T peek() {
        return queue.peek();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public int hashCode() {
        return queue.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return queue.equals(o);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return queue.addAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return queue.toArray(a);
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public Iterator<T> iterator() {
        return queue.iterator();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
