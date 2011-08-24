package java.dyn;

/**
 * Mock version of same class from Lukas Stadler's coro work.
 */
public class Coroutine extends CoroutineBase {
    public static CoroutineBase current() {
        return null;
    }
    
    public static void yieldTo(Coroutine coro) {}
    
    protected void run() {}
}
