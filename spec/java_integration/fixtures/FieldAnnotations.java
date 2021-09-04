package java_integration.fixtures;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FieldAnnotations {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Annotated {
    }

    public static List<Annotation> countAnnotated(Class cls) {
      Field[] declaredFields = cls.getDeclaredFields();
      List<Annotation> annos = new ArrayList<Annotation>();
      for (Field field: declaredFields) {
        Annotated anno = field.getAnnotation(Annotated.class);
        if (anno != null) {
          annos.add(anno);
        }
      }

      return annos;
    }
}