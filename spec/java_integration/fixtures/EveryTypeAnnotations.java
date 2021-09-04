/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2020 JRuby Contributors
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

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

import jakarta.annotation.Resource;

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
