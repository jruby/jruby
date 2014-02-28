/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.objectstorage;

import com.oracle.truffle.api.CompilerAsserts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectStorage {

    public ObjectLayout objectLayout = ObjectLayout.EMPTY;

    public static final int PRIMITIVE_STORAGE_LOCATIONS_COUNT = 14;
    protected int primitiveStorageLocation01;
    protected int primitiveStorageLocation02;
    protected int primitiveStorageLocation03;
    protected int primitiveStorageLocation04;
    protected int primitiveStorageLocation05;
    protected int primitiveStorageLocation06;
    protected int primitiveStorageLocation07;
    protected int primitiveStorageLocation08;
    protected int primitiveStorageLocation09;
    protected int primitiveStorageLocation10;
    protected int primitiveStorageLocation11;
    protected int primitiveStorageLocation12;
    protected int primitiveStorageLocation13;
    protected int primitiveStorageLocation14;

    // A bit map to indicate which primitives are set, so that they can be Nil
    protected int primitiveSetMap;

    protected Object[] objectStorageLocations;

    public ObjectLayout getObjectLayout() {
        return objectLayout;
    }

    public void allocateObjectStorageLocations() {
        CompilerAsserts.neverPartOfCompilation();

        final int objectStorageLocationsUsed = objectLayout.getObjectStorageLocationsUsed();

        if (objectStorageLocationsUsed == 0) {
            objectStorageLocations = null;
        } else {
            objectStorageLocations = new Object[objectStorageLocationsUsed];
        }
    }

    protected Map<String, Object> getFields() {
        CompilerAsserts.neverPartOfCompilation();

        if (getObjectLayout() == null) {
            return Collections.emptyMap();
        }

        final Map<String, Object> fieldsMap = new HashMap<>();

        for (Map.Entry<String, StorageLocation> entry : getObjectLayout().getAllStorageLocations().entrySet()) {
            final String name = entry.getKey();
            final StorageLocation storageLocation = entry.getValue();

            if (storageLocation.isSet(this)) {
                fieldsMap.put(name, storageLocation.read(this, true));
            }
        }

        return fieldsMap;
    }

    public String[] getFieldNames() {
        CompilerAsserts.neverPartOfCompilation();

        final Set<String> fieldNames = getFields().keySet();
        return fieldNames.toArray(new String[fieldNames.size()]);
    }

    public boolean isFieldDefined(String name) {
        CompilerAsserts.neverPartOfCompilation();

        final StorageLocation location = getObjectLayout().findStorageLocation(name);
        return location != null && location.isSet(this);
    }

    public Object getField(String name) {
        CompilerAsserts.neverPartOfCompilation();

        // Find the storage location

        final StorageLocation storageLocation = getObjectLayout().findStorageLocation(name);

        // Get the value

        if (storageLocation == null) {
            return null;
        }

        return storageLocation.read(this, true);
    }

    public void setField(String name, Object value) {
        CompilerAsserts.neverPartOfCompilation();

        // Find the storage location

        StorageLocation storageLocation = getObjectLayout().findStorageLocation(name);

        if (storageLocation == null) {
            changeLayout(objectLayout.withNewVariable(name, value.getClass()));

            storageLocation = getObjectLayout().findStorageLocation(name);
        }

        // Try to write to that storage location

        try {
            storageLocation.write(this, value);
        } catch (GeneralizeStorageLocationException e) {
            /*
             * It might not be able to store the type that we passed, if not generalize the class's
             * layout and update the layout of this object.
             */

            changeLayout(objectLayout.withGeneralisedVariable(name));

            storageLocation = getObjectLayout().findStorageLocation(name);

            // Try to write to the generalized storage location

            try {
                storageLocation.write(this, value);
            } catch (GeneralizeStorageLocationException e1) {
                // We know that we just generalized it, so this should not happen
                throw new RuntimeException("Generalised field, but it still rejected the value");
            }
        }
    }

    protected void setFields(Map<String, Object> fields) {
        CompilerAsserts.neverPartOfCompilation();

        assert fields != null;

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            final StorageLocation storageLocation = getObjectLayout().findStorageLocation(entry.getKey());
            assert storageLocation != null;

            try {
                storageLocation.write(this, entry.getValue());
            } catch (GeneralizeStorageLocationException e) {
                throw new RuntimeException("Should not have to be generalising when setting fields - " + entry.getValue().getClass().getName() + ", " +
                        storageLocation.getStoredClass().getName());
            }
        }
    }

    public void changeLayout(ObjectLayout newLayout) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, Object> fieldsMap = getFields();

        objectLayout = newLayout;

        primitiveSetMap = 0;
        allocateObjectStorageLocations();

        setFields(fieldsMap);
    }

}
