package java_integration.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodAnnotations {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Annotated {
    }

    public static List<Annotated> countAnnotated(Class cls) {
        Method[] declaredMethods = cls.getDeclaredMethods();
        List<Annotated> annos = new ArrayList<Annotated>();
        for (Method method: declaredMethods) {
            Annotated anno = method.getAnnotation(Annotated.class);
            if (anno != null) {
                annos.add(anno);
            }
        }

        return annos;
    }
}
