package org.jruby.javasupport;

/**
 * This class exists only for access-checks to set {@linkplain JavaUtil#CAN_SET_ACCESSIBLE}
 */
public class DummyForJavaUtil {

    private static final Object PRIVATE = new Object[0];
    public static final Object PUBLIC = PRIVATE;

}
