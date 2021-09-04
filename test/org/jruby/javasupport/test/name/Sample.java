package org.jruby.javasupport.test.name;

public class Sample {
    public Sample() { }

    public Sample(int param) { // @see test_backtraces.rb
        if (param == -1) {
            throw new IllegalStateException("param == -1");
        }
    }

    public static String test(int x) {
        return Integer.toHexString(x);
    }

    public static Object test(org.jruby.RubyArray array) {
        return array.length();
    }

    public static String rubyObj(org.jruby.RubyString obj) {
        return "RubyString";
    }

    public static String rubyObj(org.jruby.RubyInteger obj) {
        return "RubyInteger";
    }

    public static String rubyObj(org.jruby.RubyObject obj) {
        return "RubyObject";
    }

    public static String rubyObj(java.lang.Object obj) {
        return "";
    }

}