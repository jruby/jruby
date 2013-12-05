package org.jruby.runtime.invokedynamic;

/**
 * Represents Ruby method names invoked dynamically from Java Code.
 *
 * Adding names here will increase the size of the global method cache by the number of indexed classes in the system.
 *
 * @see org.jruby.runtime.ClassIndex
 * @see org.jruby.runtime.Helpers#invokedynamic(org.jruby.runtime.ThreadContext, org.jruby.runtime.builtin.IRubyObject, MethodNames)
*/
public enum MethodNames {
    DUMMY(""),
    OP_EQUAL("=="),
    EQL("eql?"),
    HASH("hash"),
    OP_CMP("<=>"),
    INSPECT("inspect");

    MethodNames(String realName) {
        this.realName = realName;
    }

    public String realName() {
        return realName;
    }

    final String realName;
}
