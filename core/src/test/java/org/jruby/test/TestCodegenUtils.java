package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.util.CodegenUtils.ci;

public class TestCodegenUtils extends TestCase {
    ArrayList<Event> log;
    private AnnotationVisitorLogger logger;
    private Map<String, Object> fields;

    @Override public void setUp() throws Exception {
        log = new ArrayList<Event>();
        logger = new AnnotationVisitorLogger(log);
        fields = new LinkedHashMap<String, Object>();
        super.setUp();
    }

    private static enum SimpleEnum {
        FirstValue,
        SecondValue;
    }

    private static class SimpleClass {
    }

    /** Example interface
     *
     * private static @interface SimplePrimitive {
     *   String string() default "";
     *
     *   SimpleEnum myEnum();
     *
     *   Class<?> myClass() default Object.class;
     * }
     */

    public void testvisitAnnotationFields_whenSimplePrimitive_visitsAsPrimitive() {
        fields.put("string", "object");

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(logger.getEventList().size(), 1);
        VisitEvent expectedEvent = new VisitEvent("string", "object");
        assertEquals(expectedEvent, logger.getEventList().get(0));
    }

    public void testvisitAnnotationFields_whenSimpleEnum_visitsAsEnum() {
        fields.put("myEnum", SimpleEnum.SecondValue);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(logger.getEventList().size(), 1);
        VisitEnumEvent expectedEvent = new VisitEnumEvent("myEnum", ci(SimpleEnum.class), SimpleEnum.SecondValue.name());
        assertEquals(expectedEvent, logger.getEventList().get(0));
    }

    public void testvisitAnnotationFields_whenSimpleClass_visitsAsClass() {
        fields.put("myClass", SimpleClass.class);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(logger.getEventList().size(), 1);
        VisitEvent expectedEvent = new VisitEvent("myClass", Type.getType(SimpleClass.class));
        assertEquals(expectedEvent, logger.getEventList().get(0));
    }

    public void testvisitAnnotationFields_whenMultipleSimpleFields_visitsAllFields() {
        fields.put("string", "object");
        fields.put("myEnum", SimpleEnum.SecondValue);
        fields.put("myClass", SimpleClass.class);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(3, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitEvent("string", "object"),
            new VisitEnumEvent("myEnum", ci(SimpleEnum.class), SimpleEnum.SecondValue.name()),
            new VisitEvent("myClass", Type.getType(SimpleClass.class)));
        assertEquals(expectedEventList, logger.getEventList());
    }

    public void testvisitAnnotationFields_whenArrayOfPrimitives_visitsArrayAndEachElement() {
        String[] strings = {"hello", "world"};
        fields.put("string", strings);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(4, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitArrayEvent("string"),
            new VisitEvent(null, "hello"),
            new VisitEvent(null, "world"),
            new VisitEndEvent());
        assertEquals(expectedEventList, logger.getEventList());
    }

    public void testvisitAnnotationFields_whenArrayOfEnums_visitsArrayAndEachElementAsEnums() {
        SimpleEnum[] simpleEnums = {SimpleEnum.FirstValue, SimpleEnum.SecondValue};
        fields.put("myEnum", simpleEnums);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(4, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitArrayEvent("myEnum"),
            new VisitEnumEvent(null, ci(SimpleEnum.class), SimpleEnum.FirstValue.name()),
            new VisitEnumEvent(null, ci(SimpleEnum.class), SimpleEnum.SecondValue.name()),
            new VisitEndEvent());

        assertEquals(expectedEventList, logger.getEventList());
    }

    public void testvisitAnnotationFields_whenArrayOfClasses_visitsArrayAndEachElementAsClasses() {
        Class<?>[] classes = {SimpleClass.class, Object.class};
        fields.put("myClass", classes);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(4, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitArrayEvent("myClass"),
            new VisitEvent(null, Type.getType(SimpleClass.class)),
            new VisitEvent(null, Type.getType(Object.class)),
            new VisitEndEvent());

        assertEquals(expectedEventList, logger.getEventList());
    }

    /** Example interface
     *
     * private static @interface ComplexPrimitive {
     *    String[] string() default "";
     *
     *    SimpleEnum[] myEnum();
     *
     *    Class<?>[] myClass() default Object.class;
     * }
     **/

    public void testvisitAnnotationFields_whenArrayOfMixedTypes_visitsArrayAndEachElementAsAppropriate() {
        Class<?>[] classes = {SimpleClass.class, Object.class};
        fields.put("myClass", classes);
        SimpleEnum[] simpleEnums = {SimpleEnum.FirstValue, SimpleEnum.SecondValue};
        fields.put("myEnum", simpleEnums);
        String[] strings = {"hello", "world"};
        fields.put("string", strings);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(12, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitArrayEvent("myClass"),
            new VisitEvent(null, Type.getType(SimpleClass.class)),
            new VisitEvent(null, Type.getType(Object.class)),
            new VisitEndEvent(),
            new VisitArrayEvent("myEnum"),
            new VisitEnumEvent(null, ci(SimpleEnum.class), SimpleEnum.FirstValue.name()),
            new VisitEnumEvent(null, ci(SimpleEnum.class), SimpleEnum.SecondValue.name()),
            new VisitEndEvent(),
            new VisitArrayEvent("string"),
            new VisitEvent(null, "hello"),
            new VisitEvent(null, "world"),
            new VisitEndEvent());

        assertEquals(expectedEventList, logger.getEventList());
    }


    private @interface NestedInterface {
        SimpleEnum myEnum();
    }

    /** Example interface containing a nested interface
     *
     * private @interface SimpleInterface {
     *   NestedInterface value();
     * }
     **/

    public void testvisitAnnotationFields_whenNestedAnnotations_visitsAnnotationsRecursivly() {
        Map<String, Object> nestedInterfaceFields = new LinkedHashMap<String, Object>();
        nestedInterfaceFields.put("myEnum", SimpleEnum.SecondValue);
        Map<Class, Map<String, Object>> nestedInterfaceStructure =
            new LinkedHashMap<Class, Map<String, Object>>();

        nestedInterfaceStructure.put(NestedInterface.class, nestedInterfaceFields);

        fields.put("value", nestedInterfaceStructure);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(3, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitAnnotationEvent("value", Type.getType(NestedInterface.class).getDescriptor()),
            new VisitEnumEvent("myEnum", ci(SimpleEnum.class), SimpleEnum.SecondValue.name()),
            new VisitEndEvent());

        assertEquals(expectedEventList, logger.getEventList());
    }

    public void testvisitAnnotationFields_whenSuppliedInvalidNestedAnnotationMap_throwsError() {
        Exception thrown = null;

        Map<Class, Object> nestedInterfaceStructure = new LinkedHashMap<Class, Object>();
        nestedInterfaceStructure.put(NestedInterface.class, SimpleEnum.SecondValue.name());

        fields.put("value", nestedInterfaceStructure);

        try {
            CodegenUtils.visitAnnotationFields(logger, fields);
        } catch (RuntimeException e) {
            thrown = e;
        }

        assertNotNull(thrown);
        assertEquals(CodegenUtils.InvalidAnnotationDescriptorException.class, thrown.getClass());
    }

    private @interface ComplexInterface {

    }

    public void testvisitAnnotationFields_whenArrayOfNestedAnnotations_visitsAnnotationsRecursivly() {
        Map<Class, Map<String, Object>> nestedInterfaceStructure1 =
            new LinkedHashMap<Class, Map<String, Object>>();
        Map<Class, Map<String, Object>> nestedInterfaceStructure2 =
            new LinkedHashMap<Class, Map<String, Object>>();

        Map<String, Object> nestedInterfaceFields = new LinkedHashMap<String, Object>();
        nestedInterfaceFields.put("myEnum", SimpleEnum.SecondValue);

        Map<String, Object> nestedInterfaceFields2 = new LinkedHashMap<String, Object>();
        nestedInterfaceFields2.put("myEnum", SimpleEnum.FirstValue);

        nestedInterfaceStructure1.put(NestedInterface.class, nestedInterfaceFields);
        nestedInterfaceStructure2.put(NestedInterface.class, nestedInterfaceFields2);

        Map[] fieldArray = {nestedInterfaceStructure1, nestedInterfaceStructure2};

        fields.put("value", fieldArray);

        CodegenUtils.visitAnnotationFields(logger, fields);

        assertEquals(8, logger.getEventList().size());
        List<Event> expectedEventList = Arrays.asList(
            new VisitArrayEvent("value"),
            new VisitAnnotationEvent(null, Type.getType(NestedInterface.class).getDescriptor()),
            new VisitEnumEvent("myEnum", ci(SimpleEnum.class), SimpleEnum.SecondValue.name()),
            new VisitEndEvent(),
            new VisitAnnotationEvent(null, Type.getType(NestedInterface.class).getDescriptor()),
            new VisitEnumEvent("myEnum", ci(SimpleEnum.class), SimpleEnum.FirstValue.name()),
            new VisitEndEvent(),
            new VisitEndEvent());

        assertEquals(expectedEventList, logger.getEventList());
    }

    private static interface Event {

    }

    private static class VisitEvent implements Event {
        private final String key;
        private final Object object;

        VisitEvent(String key, Object value) {
            this.key = key;
            this.object = value;
        }

        public String getKey() {
            return key;
        }

        public Object getObject() {
            return object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VisitEvent that = (VisitEvent) o;

            if (object != null ? !object.equals(that.object) : that.object != null) return false;
            if (key != null ? !key.equals(that.key) : that.key != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (object != null ? object.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "VisitEvent{" +
                "key='" + key + '\'' +
                ", object=" + object +
                '}';
        }
    }

    private static class VisitEnumEvent implements Event {
        private final String key;
        private final String enumClass;
        private final String value;

        VisitEnumEvent(String key, String enumClass, String value) {
            this.key = key;
            this.enumClass = enumClass;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getEnumClass() {
            return enumClass;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VisitEnumEvent that = (VisitEnumEvent) o;

            if (enumClass != null ? !enumClass.equals(that.enumClass) : that.enumClass != null) {
                return false;
            }
            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (enumClass != null ? enumClass.hashCode() : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "VisitEnumEvent{" +
                "key='" + key + '\'' +
                ", enumClass='" + enumClass + '\'' +
                ", value='" + value + '\'' +
                '}';
        }
    }

    private static class VisitArrayEvent implements Event {
        private final String key;

        VisitArrayEvent(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VisitArrayEvent that = (VisitArrayEvent) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key != null ? key.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "VisitArrayEvent{" +
                "key='" + key + '\'' +
                '}';
        }
    }

    private static class VisitEndEvent implements Event {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return true;
        }

        @Override
        public String toString() {
            return "VisitEndEvent{}";
        }
    }

    private static class VisitAnnotationEvent implements Event {
        private final String key;
        private final String value;

        public VisitAnnotationEvent(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VisitAnnotationEvent that = (VisitAnnotationEvent) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "VisitAnnotationEvent{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
        }
    }

    private static class AnnotationVisitorLogger extends AnnotationVisitor {
        private final List<Event> eventList;

        public AnnotationVisitorLogger(List<Event> eventList) {
            super(Opcodes.ASM4);
            this.eventList = eventList;
        }

        public void visit(String s, Object o) {
            eventList.add(new VisitEvent(s, o));
        }

        public void visitEnum(String s, String s2, String s3) {
            eventList.add(new VisitEnumEvent(s, s2, s3));
        }

        public AnnotationVisitor visitAnnotation(String s, String s2) {
            eventList.add(new VisitAnnotationEvent(s, s2));
            return this;
        }

        public AnnotationVisitor visitArray(String s) {
            eventList.add(new VisitArrayEvent(s));
            return this;
        }

        public void visitEnd() {
            eventList.add(new VisitEndEvent());
        }

        public List<Event> getEventList() {
            return eventList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AnnotationVisitorLogger that = (AnnotationVisitorLogger) o;

            if (!eventList.equals(that.eventList)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return eventList.hashCode();
        }
    }
}
