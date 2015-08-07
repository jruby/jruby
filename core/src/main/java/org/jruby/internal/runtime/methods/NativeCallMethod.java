package org.jruby.internal.runtime.methods;


/**
 * Created by headius on 8/1/15.
 */
public interface NativeCallMethod {
    /**
     * Set the single-arity NativeCall for this method. All signatures for the
     * non-single-arity getNativeCall will also be set to this value.
     *
     * @param nativeTarget native method target
     * @param nativeName native method name
     * @param nativeReturn native method return
     * @param nativeSignature native method arguments
     * @param statik static?
     * @param java plain Java method?
     */
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik, boolean java);

    /**
     * Set the single-arity NativeCall for this method. All signatures for the
     * non-single-arity getNativeCall will also be set to this value.
     *
     * @param nativeTarget native method target
     * @param nativeName native method name
     * @param nativeReturn native method return
     * @param nativeSignature native method arguments
     * @param statik static?
     */
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik);

    /**
     * Get the NativeCall for the method, if available.
     *
     * @return a NativeCall if the method has a native representation.
     */
    public DynamicMethod.NativeCall getNativeCall();
}
