package dorkbox.util.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * for specifying the default report methods, without constantly creating new objects
 */
public class AnnotationDefaults {
    public static final ReporterFunction<String> getTypeName = new ReporterFunction<String>() {
        @Override
        public String report(Cursor cursor) {
            return cursor.getTypeName();
        }
    };
    public static final ReporterFunction<Class<? extends Annotation>> getAnnotationType = new ReporterFunction<Class<? extends Annotation>>() {
        @Override
        public Class<? extends Annotation> report(Cursor cursor) {
            return cursor.getAnnotationType();
        }
    };
    public static final ReporterFunction<ElementType> getElementType = new ReporterFunction<ElementType>() {
        @Override
        public ElementType report(Cursor cursor) {
            return cursor.getElementType();
        }
    };
    public static final ReporterFunction<String> getMemberName = new ReporterFunction<String>() {
        @Override
        public String report(Cursor cursor) {
            return cursor.getMemberName();
        }
    };
    public static final ReporterFunction<Class<?>> getType = new ReporterFunction<Class<?>>() {
        @Override
        public Class<?> report(Cursor cursor) {
            return cursor.getType();
        }
    };
    public static final ReporterFunction<Constructor<?>> getConstructor = new ReporterFunction<Constructor<?>>() {
        @Override
        public Constructor<?> report(Cursor cursor) {
            return cursor.getConstructor();
        }
    };
    public static final ReporterFunction<Field> getField = new ReporterFunction<Field>() {
        @Override
        public Field report(Cursor cursor) {
            return cursor.getField();
        }
    };
    public static final ReporterFunction<Method> getMethod = new ReporterFunction<Method>() {
        @Override
        public Method report(Cursor cursor) {
            return cursor.getMethod();
        }
    };
}
