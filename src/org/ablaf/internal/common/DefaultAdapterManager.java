/*
 * DefaultAdapterManager.java
 * Created on 08.02.2002, 00:11:30
 * 
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "AbLaF" and "Abstract Language Framework" must not be 
 *    used to endorse or promote products derived from this software 
 *    without prior written permission. For written permission, please
 *    contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called 
 *    "Abstract Language Framework", nor may 
 *    "Abstract Language Framework" appear in their name, without prior 
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * ====================================================================
 *
 */
package org.ablaf.internal.common;

import org.ablaf.common.IAdaptable;
import org.ablaf.common.IAdapterFactory;
import org.ablaf.common.IAdapterManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefaultAdapterManager implements IAdapterManager {
    private Map factoryMap = new HashMap(5);
    
    private static class FactoryMapKey {
        private Class adaptable;
        private Class adapterType;

        public FactoryMapKey(Class adaptable, Class adapterType) {
            this.adaptable = adaptable;
            this.adapterType = adapterType;
        }
        
        public boolean equals(Object other) {
            if (other instanceof FactoryMapKey) {
                return this.adaptable == ((FactoryMapKey)other).adaptable &&
                       this.adapterType == ((FactoryMapKey)other).adapterType;
            }
            return false;
        }
        
        public int hashCode() {
            return adaptable.hashCode() ^ adapterType.hashCode();
        }
    }
    

    private void addFactoryToMap(IAdapterFactory factory, Class adaptable) {
        for (int i = 0; i < factory.getAdapterList().length; i++) {
            Class adapterType = factory.getAdapterList()[i];
            
            factoryMap.put(new FactoryMapKey(adaptable, adapterType), factory);
        }
    }

    private void removeFactoryFromMap(IAdapterFactory factory, Class adaptable) {
        for (int i = 0; i < factory.getAdapterList().length; i++) {
            Class adapterType = factory.getAdapterList()[i];
            
            factoryMap.remove(new FactoryMapKey(adaptable, adapterType));
        }
    }

    /**
     * @see IAdapterManager#getAdapter(IAdaptable, Class)
     */
    public Object getAdapter(IAdaptable adaptableObject, Class adapterType) {
        IAdapterFactory factory = (IAdapterFactory)factoryMap.get(new FactoryMapKey(adaptableObject.getClass(), adapterType));
        if (factory != null) {
            return factory.getAdapter(adaptableObject, adapterType);
        } else {
            return null;
	    }
    }

    /**
     * @see IAdapterManager#registerAdapters(IAdapterFactory, Class)
     */
    public void registerAdapters(IAdapterFactory factory, Class adaptable) {
        addFactoryToMap(factory, adaptable);
    }

    /**
     * @see IAdapterManager#unregisterAdapters(IAdapterFactory)
     */
    public void unregisterAdapters(IAdapterFactory factory) {
        Iterator iter = factoryMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getValue() == factory) {
                iter.remove();
            }
        }
    }

    /**
     * @see IAdapterManager#unregisterAdapters(IAdapterFactory, Class)
     */
    public void unregisterAdapters(IAdapterFactory factory, Class adaptable) {
        removeFactoryFromMap(factory, adaptable);
    }
}