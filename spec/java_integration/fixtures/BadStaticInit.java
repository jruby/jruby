public class BadStaticInit {
    static {
        Object foo = null;
        foo.toString();
    }
}
