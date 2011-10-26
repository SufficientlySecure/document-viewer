package org.ebookdroid.core.actions;

import org.ebookdroid.utils.LengthUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class ActionInitControllerMethod {

    private static ActionInitControllerMethod s_instance;

    private static HashMap<Class<?>, Map<String, Method>> s_methods = new HashMap<Class<?>, Map<String, Method>>();

    public static ActionInitControllerMethod getInstance() {
        if (s_instance == null) {
            s_instance = new ActionInitControllerMethod();
        }
        return s_instance;
    }

    /**
     * Constructor
     * 
     */
    private ActionInitControllerMethod() {
    }

    /**
     * Invokes controller method for the given controller and action
     * 
     * @param action
     *            action
     * @return execution result
     * @throws Throwable
     *             thrown by reflection API or executed method
     */
    Object invoke(IActionController<?> controller, ActionEx action) throws Throwable {
        final Object m[] = getInitCall(controller, action.getId());
        if (m[0] != null) {
            return ((Method) m[0]).invoke(m[1], action);
        }
        return null;
    }

    /**
     * @return {@link Method}
     */
    Object[] getInitCall(IActionController<?> controller, String actionId) {
        Object[] call = { null, null };
        for (IActionController<?> c = controller; call[0] == null && c != null; c = c.getParent()) {
            call[0] = getMethod(c.getManagedComponent(), actionId);
            if (call[0] != null) {
                call[1] = c.getManagedComponent();
            } else {
                call[0] = getMethod(c, actionId);

                if (call[0] != null) {
                    call[1] = c;
                }
            }
        }
        return call;

    }

    /**
     * Gets the method.
     * 
     * @param target
     *            a possible action target
     * @param actionId
     *            the action id
     * 
     * @return the method
     */
    private static synchronized Method getMethod(final Object target, final String actionId) {
        final Class<? extends Object> clazz = target.getClass();

        Map<String, Method> methods = s_methods.get(clazz);
        if (methods == null) {
            methods = getActionMethods(clazz);
            s_methods.put(clazz, methods);
        }
        return methods.get(actionId);
    }

    /**
     * Gets the method.
     * 
     * @param clazz
     *            an action target class
     * 
     * @return the map of action methods method
     */
    private static Map<String, Method> getActionMethods(final Class<?> clazz) {
        final HashMap<String, Method> result = new HashMap<String, Method>();

        final Method[] methods = clazz.getMethods();
        for (final Method method : methods) {
            final int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                final Class<?>[] args = method.getParameterTypes();
                if (LengthUtils.length(args) == 1 && ActionEx.class.equals(args[0])) {
                    final ActionInitMethod initAnnotation = method.getAnnotation(ActionInitMethod.class);
                    if (initAnnotation != null) {
                        final String[] ids = initAnnotation.ids();
                        if (LengthUtils.isNotEmpty(ids)) {
                            for (String id : ids) {
                                result.put(id, method);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
