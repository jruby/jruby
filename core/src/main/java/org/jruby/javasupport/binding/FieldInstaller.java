package org.jruby.javasupport.binding;

import java.lang.reflect.Field;

/**
* Created by headius on 2/26/15.
*/
public abstract class FieldInstaller extends NamedInstaller {

    final Field field;

    public FieldInstaller(String name, int type, Field field) {
        super(name,type);
        this.field = field;
    }
}
