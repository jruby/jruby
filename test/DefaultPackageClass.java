public class DefaultPackageClass {

    public transient int x;
    public static long anY = 1L;

    public String foo() { return "foo"; }

    public static int compareTo(org.jruby.RubyObject o1, org.jruby.RubyObject o2) {
        if ( o1 == null ) { // JI always converts nil to null
            o1 = (org.jruby.RubyNil) o2.getRuntime().getNil();
        }
        if ( o2 == null ) { // JI always converts nil to null
            o2 = (org.jruby.RubyNil) o1.getRuntime().getNil();
        }
        return ((Comparable) o1).compareTo(o2);
    }

    public static Class<?> returnLongClass() { return Long.class; }

}
