/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jvyamlb;

import java.io.FileInputStream;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.jvyamlb.events.AliasEvent;
import org.jvyamlb.events.Event;
import org.jvyamlb.events.NodeEvent;
import org.jvyamlb.events.MappingEndEvent;
import org.jvyamlb.events.MappingStartEvent;
import org.jvyamlb.events.ScalarEvent;
import org.jvyamlb.events.SequenceStartEvent;
import org.jvyamlb.events.SequenceEndEvent;
import org.jvyamlb.events.StreamStartEvent;
import org.jvyamlb.events.StreamEndEvent;

import org.jvyamlb.nodes.Node;
import org.jvyamlb.nodes.ScalarNode;
import org.jvyamlb.nodes.SequenceNode;
import org.jvyamlb.nodes.MappingNode;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class ComposerImpl implements Composer {
    private Parser parser;
    private Resolver resolver;
    private Map anchors;

    public ComposerImpl(final Parser parser, final Resolver resolver) {
        this.parser = parser;
        this.resolver = resolver;
        this.anchors = new HashMap();
    }

    public boolean checkNode() {
        return !(parser.peekEvent() instanceof StreamEndEvent);
    }
    
    public Node getNode() {
        return checkNode() ? composeDocument() : (Node)null;
    }

    private class NodeIterator implements Iterator {
        public boolean hasNext() {return checkNode();}
        public Object next() {return getNode();}
        public void remove() {}
    }

    public Iterator eachNode() {
        return new NodeIterator();
    }

    public Iterator iterator() {
        return eachNode();
    }

    public Node composeDocument() {
        if(parser.peekEvent() instanceof StreamStartEvent) {
            //Drop STREAM-START event
            parser.getEvent();
        }
        //Drop DOCUMENT-START event
        parser.getEvent();
        final Node node = composeNode(null,null);
        //Drop DOCUMENT-END event
        parser.getEvent();
        this.anchors.clear();
        return node;
    }

    private final static boolean[] FALS = new boolean[]{false};
    private final static boolean[] TRU = new boolean[]{true};

    public Node composeNode(final Node parent, final Object index) {
        if(parser.peekEvent() instanceof AliasEvent) {
            final AliasEvent event = (AliasEvent)parser.getEvent();
            final String anchor = event.getAnchor();
            if(!anchors.containsKey(anchor)) {
                throw new ComposerException(null,"found undefined alias " + anchor,null);
            }
            return (Node)anchors.get(anchor);
        }
        final Event event = parser.peekEvent();
        String anchor = null;
        if(event instanceof NodeEvent) {
            anchor = ((NodeEvent)event).getAnchor();
        }
        resolver.descendResolver(parent,index);
        Node node = null;
        if(event instanceof ScalarEvent) {
            final ScalarEvent ev = (ScalarEvent)parser.getEvent();
            String tag = ev.getTag();
            if(tag == null || tag.equals("!")) {
                tag = resolver.resolve(ScalarNode.class,ev.getValue(),ev.getImplicit());
            }
            node = new ScalarNode(tag,ev.getValue(),ev.getStyle());
            if(null != anchor) {
                anchors.put(anchor,node);
            }
        } else if(event instanceof SequenceStartEvent) {
            final SequenceStartEvent start = (SequenceStartEvent)parser.getEvent();
            String tag = start.getTag();
            if(tag == null || tag.equals("!")) {
                tag = resolver.resolve(SequenceNode.class,null,start.getImplicit()  ? TRU : FALS);
            }
            node = new SequenceNode(tag,new ArrayList(),start.getFlowStyle());
            if(null != anchor) {
                anchors.put(anchor,node);
            }
            int ix = 0;
            while(!(parser.peekEvent() instanceof SequenceEndEvent)) {
                ((List)node.getValue()).add(composeNode(node,new Integer(ix++)));
            }
            parser.getEvent();
        } else if(event instanceof MappingStartEvent) {
            final MappingStartEvent start = (MappingStartEvent)parser.getEvent();
            String tag = start.getTag();
            if(tag == null || tag.equals("!")) {
                tag = resolver.resolve(MappingNode.class,null, start.getImplicit() ? TRU : FALS);
            }
            node = new MappingNode(tag, new HashMap(), start.getFlowStyle());
            if(null != anchor) {
                anchors.put(anchor,node);
            }
            while(!(parser.peekEvent() instanceof MappingEndEvent)) {
                final Event key = parser.peekEvent();
                final Node itemKey = composeNode(node,null);
                if(((Map)node.getValue()).containsKey(itemKey)) {
                    composeNode(node,itemKey);
                } else {
                    ((Map)node.getValue()).put(itemKey,composeNode(node,itemKey));
                }
            }
            parser.getEvent();
        }
        resolver.ascendResolver();
        return node;
    }
    
    public static void main(final String[] args) throws Exception {
        final String filename = args[0];
        System.out.println("Reading of file: \"" + filename + "\"");

        final ByteList input = new ByteList(1024);
        final InputStream reader = new FileInputStream(filename);
        byte[] buff = new byte[1024];
        int read = 0;
        while(true) {
            read = reader.read(buff);
            input.append(buff,0,read);
            if(read < 1024) {
                break;
            }
        }
        reader.close();
        final long before = System.currentTimeMillis();
        for(int i=0;i<1;i++) {
            final Composer cmp = new ComposerImpl(new ParserImpl(new ScannerImpl(input)),new ResolverImpl());
            for(final Iterator iter = cmp.eachNode();iter.hasNext();) {
                iter.next();
                //                System.out.println(iter.next());
            }
        }
        final long after = System.currentTimeMillis();
        final long time = after-before;
        final double timeS = (after-before)/1000.0;
        System.out.println("Walking through the nodes for the file: " + filename + " took " + time + "ms, or " + timeS + " seconds"); 
    }
}// ComposerImpl
