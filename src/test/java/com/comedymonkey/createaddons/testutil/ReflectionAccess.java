package com.comedymonkey.createaddons.testutil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Calls methods and reads or writes fields by name, for members a test can't reach directly: private ones of the
 * code under test, and protected ones Create declares in its own packages (such as a kinetic block entity's speed).
 * Methods are matched by name and by the runtime types of the arguments.
 */
public final class ReflectionAccess {
	private ReflectionAccess() {}

	/** Calls the instance method with this name that takes these arguments, wherever it is declared. */
	public static Object call(Object target, String name, Object... args) {
		return invoke(find(target.getClass(), name, args), target, args);
	}

	public static Object get(Object target, String field) {
		try {
			return field(target.getClass(), field).get(target);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void set(Object target, String field, Object value) {
		try {
			field(target.getClass(), field).set(target, value);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Field field(Class<?> type, String name) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			try {
				Field field = c.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
				// Keep looking in the superclass
			}
		}
		throw new IllegalArgumentException("No field " + name + " in " + type.getName());
	}

	private static Method find(Class<?> type, String name, Object[] args) {
		for (Class<?> c = type; c != null; c = c.getSuperclass())
			for (Method method : c.getDeclaredMethods())
				if (method.getName().equals(name) && !method.isBridge() && accepts(method.getParameterTypes(), args)) {
					method.setAccessible(true);
					return method;
				}
		throw new IllegalArgumentException("No method " + name + " in " + type.getName() + " takes those arguments");
	}

	private static Object invoke(Method method, Object target, Object[] args) {
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw rethrow(e.getCause());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean accepts(Class<?>[] parameters, Object[] args) {
		if (parameters.length != args.length)
			return false;
		for (int i = 0; i < parameters.length; i++) {
			Class<?> parameter = parameters[i];
			if (args[i] == null) {
				if (parameter.isPrimitive())
					return false;
			} else if (!wrapper(parameter).isInstance(args[i])) {
				return false;
			}
		}
		return true;
	}

	private static Class<?> wrapper(Class<?> type) {
		if (!type.isPrimitive())
			return type;
		if (type == int.class)
			return Integer.class;
		if (type == boolean.class)
			return Boolean.class;
		if (type == float.class)
			return Float.class;
		if (type == double.class)
			return Double.class;
		if (type == long.class)
			return Long.class;
		if (type == short.class)
			return Short.class;
		if (type == byte.class)
			return Byte.class;
		if (type == char.class)
			return Character.class;
		return Void.class;
	}

	private static RuntimeException rethrow(Throwable cause) {
		if (cause instanceof RuntimeException runtime)
			return runtime;
		if (cause instanceof Error error)
			throw error;
		return new IllegalStateException(cause);
	}
}
