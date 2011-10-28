package org.ebookdroid.core.actions;

import org.ebookdroid.utils.LengthUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ActionControllerMethod {

    private static HashMap<Class<?>, Map<Integer, Method>> s_methods = new HashMap<Class<?>, Map<Integer, Method>>();

    private final IActionController<?> m_controller;

    private final int m_actionId;

    private Object m_target;

    private Method m_method;

    private Throwable m_errorInfo;

    /**
     * Constructor
     * 
     * @param controller
     *            action controller
     * @param actionId
     *            action id
     */
    ActionControllerMethod(final IActionController<?> controller, final int actionId) {
        m_controller = controller;
        m_actionId = actionId;
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
    public Object invoke(final ActionEx action) throws Throwable {
        final Method m = getMethod();
        if (m != null) {
            return m.invoke(m_target, action);
        } else {
            throw m_errorInfo;
        }
    }

    /**
     * @return <code>true</code> if method is exist
     */
    public boolean isValid() {
        return null != getMethod();
    }

    /**
     * Returns reflection error info.
     * 
     * @return {@link Throwable}
     */
    public Throwable getErrorInfo() {
        getMethod();
        return m_errorInfo;
    }

    /**
     * @return {@link Method}
     */
    Method getMethod() {
        if (m_method == null && m_errorInfo == null) {
            m_method = getMethod(m_controller.getManagedComponent(), m_actionId);
            m_target = m_method != null ? m_controller.getManagedComponent() : null;

            for (IActionController<?> c = m_controller; m_method == null && c != null; c = c.getParent()) {
                m_method = getMethod(c.getManagedComponent(), m_actionId);
                m_target = m_method != null ? c.getManagedComponent() : null;
                if (m_method == null) {
                    m_method = getMethod(c, m_actionId);
                    m_target = m_method != null ? c : null;
                }
            }

            if (m_method == null) {
                String text = "No appropriate method found for action " + m_actionId + " in class "
                        + m_controller.getClass();
                m_errorInfo = new NoSuchMethodException(text);
            }
        }
        return m_method;
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
    private static synchronized Method getMethod(final Object target, final int actionId) {
        Class<? extends Object> clazz = target.getClass();

        Map<Integer, Method> methods = s_methods.get(clazz);
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
    private static Map<Integer, Method> getActionMethods(final Class<?> clazz) {
        final Map<Integer, Method> result = new HashMap<Integer, Method>();

        final Method[] methods = clazz.getMethods();
        for (final Method method : methods) {
            final int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                final Class<?>[] args = method.getParameterTypes();
                if (LengthUtils.length(args) == 1 && ActionEx.class.equals(args[0])) {
                    final ActionMethod annotation = method.getAnnotation(ActionMethod.class);
                    if (annotation != null) {
                        for (int id : annotation.ids()) {
                            result.put(id, method);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        Method m = getMethod();
        return m_actionId + ", " + (m != null ? m : "no method: " + m_errorInfo.getMessage());
    }
}
