package java_integration.fixtures;

public class UsesDescendantOfSingleMethodInterface {
    public static Object callThat(DescendantOfSingleMethodInterface obj) {
        return obj.callThat();
    }
    public static Object castAndCallIt(Object obj) {
        return callThat((DescendantOfSingleMethodInterface) obj);
    }
}