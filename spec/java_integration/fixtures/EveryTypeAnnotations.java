package java_integration.fixtures;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

public class EveryTypeAnnotations {
	@Retention(RUNTIME)
	@Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER,
			TYPE_USE })
	public @interface Annotated
	{
		String astr() default "none";
		byte abyte() default 0x00;
		short ashort() default 0;
		int anint() default 0;
		long along() default 0;
		float afloat() default 0;
		double adouble() default 0;
		boolean abool() default false;
		boolean anbool() default true;
		char achar() default 0;
		RetentionPolicy anenum() default RetentionPolicy.CLASS;
		Class<?> aClass()  default java.lang.Object.class;
		Resource[] Darray() default {};
	}

    public static Map<String, List<Object>> decodeAnnotatedMethods(Class cls) {
        Method[] declaredMethods = cls.getDeclaredMethods();
        Map<String, List<Object>> decoded = new HashMap<>();
        for (Method method: declaredMethods) {
           Annotated annotation = method.getAnnotation(Annotated.class);
           if (annotation == null)
        	   continue;
           
            // bring to native types to cause type errors if possible   
       		String astr = annotation.astr();
    		byte abyte = annotation.abyte();
    		short ashort = annotation.ashort();
    		int anint = annotation.anint();
    		long along = annotation.along();
    		float afloat = annotation.afloat();
    		double adouble = annotation.adouble();
    		boolean abool = annotation.abool();
    		boolean anbool = annotation.anbool();
    		char achar = annotation.achar();
    		RetentionPolicy anenum = annotation.anenum();
    		Class<?> aClass = annotation.aClass();
    		Resource[] Darray = annotation.Darray();
    		
    		
    		decoded.put(method.getName(), Arrays.asList(astr, abyte, ashort, anint, along, afloat, adouble, abool, anbool, achar, anenum, aClass, Darray));
          
        }

        return decoded;
    }
}
