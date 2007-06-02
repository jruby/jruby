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

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.jruby.util.ByteList;

/**
 * The combinatorial explosion class.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class YAML {
    public static final String DEFAULT_SCALAR_TAG = "tag:yaml.org,2002:str";
    public static final String DEFAULT_SEQUENCE_TAG = "tag:yaml.org,2002:seq";
    public static final String DEFAULT_MAPPING_TAG = "tag:yaml.org,2002:map";

    public static final Map ESCAPE_REPLACEMENTS = new HashMap();

    static {
        ESCAPE_REPLACEMENTS.put(new Character('\0'),"0");
        ESCAPE_REPLACEMENTS.put(new Character('\u0007'),"a");
        ESCAPE_REPLACEMENTS.put(new Character('\u0008'),"b");
        ESCAPE_REPLACEMENTS.put(new Character('\u0009'),"t");
        ESCAPE_REPLACEMENTS.put(new Character('\n'),"n");
        ESCAPE_REPLACEMENTS.put(new Character('\u000B'),"v");
        ESCAPE_REPLACEMENTS.put(new Character('\u000C'),"f");
        ESCAPE_REPLACEMENTS.put(new Character('\r'),"r");
        ESCAPE_REPLACEMENTS.put(new Character('\u001B'),"e");
        ESCAPE_REPLACEMENTS.put(new Character('"'),"\"");
        ESCAPE_REPLACEMENTS.put(new Character('\\'),"\\");
        ESCAPE_REPLACEMENTS.put(new Character('\u0085'),"N");
        ESCAPE_REPLACEMENTS.put(new Character('\u00A0'),"_");
    }
    public static YAMLConfig config() {
        return defaultConfig();
    }

    public static YAMLConfig defaultConfig() {
        return new DefaultYAMLConfig();
    }

    public static ByteList dump(final Object data) {
        return dump(data,config());
    }

    public static ByteList dump(final Object data, final YAMLFactory fact) {
        return dump(data,fact, config());
    }

    public static ByteList dump(final Object data, final YAMLConfig cfg) {
        final List lst = new ArrayList(1);
        lst.add(data);
        return dumpAll(lst,cfg);
    }

    public static ByteList dump(final Object data, final YAMLFactory fact, final YAMLConfig cfg) {
        final List lst = new ArrayList(1);
        lst.add(data);
        return dumpAll(lst,fact,cfg);
    }

    public static ByteList dumpAll(final List data) {
        return dumpAll(data,config());
    }

    public static ByteList dumpAll(final List data, final YAMLFactory fact) {
        return dumpAll(data,fact,config());
    }

    public static ByteList dumpAll(final List data, final YAMLConfig cfg) {
        final ByteArrayOutputStream swe = new ByteArrayOutputStream();
        dumpAll(data,swe,cfg);
        return new ByteList(swe.toByteArray(),false);
    }

    public static ByteList dumpAll(final List data, final YAMLFactory fact, final YAMLConfig cfg) {
        final ByteArrayOutputStream swe = new ByteArrayOutputStream();
        dumpAll(data,swe,fact,cfg);
        return new ByteList(swe.toByteArray(),false);
    }

    public static void dump(final Object data, final OutputStream output) {
        dump(data,output,config());
    }

    public static void dump(final Object data, final OutputStream output, YAMLFactory fact) {
        dump(data,output,fact,config());
    }

    public static void dump(final Object data, final OutputStream output, final YAMLConfig cfg) {
        final List lst = new ArrayList(1);
        lst.add(data);
        dumpAll(lst,output,cfg);
    }

    public static void dump(final Object data, final OutputStream output, final YAMLFactory fact, final YAMLConfig cfg) {
        final List lst = new ArrayList(1);
        lst.add(data);
        dumpAll(lst,output,fact,cfg);
    }

    public static void dumpAll(final List data, final OutputStream output) {
        dumpAll(data,output,config());
    }

    public static void dumpAll(final List data, final OutputStream output, final YAMLFactory fact) {
        dumpAll(data,output,fact,config());
    }

    public static void dumpAll(final List data, final OutputStream output, final YAMLConfig cfg) {
        dumpAll(data,output,new DefaultYAMLFactory(),cfg);
    }

    public static void dumpAll(final List data, final OutputStream output, final YAMLFactory fact, final YAMLConfig cfg) {
        final Serializer s = fact.createSerializer(fact.createEmitter(output,cfg),fact.createResolver(),cfg);
        try {
            s.open();
            final Representer r = fact.createRepresenter(s,cfg);
            for(final Iterator iter = data.iterator(); iter.hasNext();) {
                r.represent(iter.next());
            }
        } catch(final java.io.IOException e) {
            throw new YAMLException(e);
        } finally {
            try { s.close(); } catch(final java.io.IOException e) {}
        }
    }

    public static Object load(final ByteList io) {
        return load(io, new DefaultYAMLFactory(),config());
    }

    public static Object load(final InputStream io) {
        return load(io, new DefaultYAMLFactory(),config());
    }

    public static Object load(final ByteList io, final YAMLConfig cfg) {
        return load(io, new DefaultYAMLFactory(),cfg);
    }

    public static Object load(final InputStream io, final YAMLConfig cfg) {
        return load(io, new DefaultYAMLFactory(),cfg);
    }

    public static Object load(final ByteList io, final YAMLFactory fact, final YAMLConfig cfg) {
        final Constructor ctor = fact.createConstructor(fact.createComposer(fact.createParser(fact.createScanner(io),cfg),fact.createResolver()));
        if(ctor.checkData()) {
            return ctor.getData();
        } else {
            return null;
        }
    }

    public static Object load(final InputStream io, final YAMLFactory fact, final YAMLConfig cfg) {
        final Constructor ctor = fact.createConstructor(fact.createComposer(fact.createParser(fact.createScanner(io),cfg),fact.createResolver()));
        if(ctor.checkData()) {
            return ctor.getData();
        } else {
            return null;
        }
    }

    public static List loadAll(final ByteList io) {
        return loadAll(io, new DefaultYAMLFactory(),config());
    }

    public static List loadAll(final InputStream io) {
        return loadAll(io, new DefaultYAMLFactory(),config());
    }

    public static List loadAll(final ByteList io, final YAMLConfig cfg) {
        return loadAll(io, new DefaultYAMLFactory(),cfg);
    }

    public static List loadAll(final InputStream io, final YAMLConfig cfg) {
        return loadAll(io, new DefaultYAMLFactory(),cfg);
    }

    public static List loadAll(final ByteList io, final YAMLFactory fact, final YAMLConfig cfg) {
        final List result = new ArrayList();
        final Constructor ctor = fact.createConstructor(fact.createComposer(fact.createParser(fact.createScanner(io),cfg),fact.createResolver()));
        while(ctor.checkData()) {
            result.add(ctor.getData());
        }
        return result;
    }

    public static List loadAll(final InputStream io, final YAMLFactory fact, final YAMLConfig cfg) {
        final List result = new ArrayList();
        final Constructor ctor = fact.createConstructor(fact.createComposer(fact.createParser(fact.createScanner(io),cfg),fact.createResolver()));
        while(ctor.checkData()) {
            result.add(ctor.getData());
        }
        return result;
    }

    public static void main(final String[] args) {
        System.err.println(YAML.load(new ByteList("%YAML 1.0\n---\n!str str".getBytes())));
    }
}// YAML
