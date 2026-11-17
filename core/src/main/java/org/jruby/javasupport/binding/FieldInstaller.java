package org.jruby.javasupport.binding;

import org.jruby.javasupport.Java;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
* Created by headius on 2/26/15.
*/
public abstract class FieldInstaller extends NamedInstaller {

    final Field field;

    public FieldInstaller(String name, int type, Field field) {
        super(name,type);
        this.field = field;
    }

    public boolean isAccessible() {
        return Modifier.isPublic(field.getModifiers()) || Java.trySetAccessible(field);
    }
}
