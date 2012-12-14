package java_integration.fixtures;

import java.lang.ArrayStoreException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParameterAnnotations {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Annotated {
    }

    public static List<Annotation> countAnnotated(Class cls) {
        Method[] declaredMethods = cls.getDeclaredMethods();
        List<Annotation> annos = new ArrayList<Annotation>();
        for (Method method: declaredMethods) {
           for (Annotation[] annotations : method.getParameterAnnotations()) {
             for (Annotation annotation : annotations) {
               annos.add(annotation);
             }
          }
        }

        return annos;
    }
}
