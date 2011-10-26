package org.ebookdroid.core.actions;

import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.utils.LengthUtils;

import android.app.Activity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class EventDispatcher {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Events");

    private final Activity m_base;

    private final InvokationType m_type;

    private final Object m_target;

    private Object m_proxy;

    private InvocationHandler m_handler;

    /**
     * Constructor
     * 
     * @param type
     *            invocation type
     * @param target
     *            target object
     * @param listeners
     *            a list of listener interfaces
     */
    public EventDispatcher(final Activity base, final InvokationType type, final Object target,
            final Class<?>... listeners) {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }

        if (LengthUtils.isEmpty(listeners)) {
            throw new IllegalArgumentException("Listeners list cannot be empty");
        }

        for (final Class<?> listener : listeners) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener class cannot be null");
            }
            if (!listener.isInterface()) {
                throw new IllegalArgumentException("Listener class should be an interface");
            }
            if (!listener.isInstance(target)) {
                throw new IllegalArgumentException("Target should be an instance of the listener class");
            }
        }

        m_base = base;
        m_type = type;
        m_target = target;
        m_handler = new Handler();
        m_proxy = Proxy.newProxyInstance(target.getClass().getClassLoader(), listeners, m_handler);
    }

    /**
     * Gets a listener of the given type.
     * 
     * @param <Listener>
     *            listener type
     * @return listener proxy object casted to the given type
     */
    @SuppressWarnings("unchecked")
    public <Listener> Listener getListener() {
        return (Listener) m_proxy;
    }

    /**
     * This class implements invocation handler for event listeners.
     */
    private class Handler implements InvocationHandler {

        /**
         * Processes a method invocation on a proxy instance and returns the
         * result.
         * 
         * @param proxy
         *            the proxy instance that the method was invoked on
         * @param method
         *            the <code>Method</code> instance corresponding to
         *            the interface method invoked on the proxy instance.
         * @param args
         *            an array of objects containing the values of the
         *            arguments passed in the method invocation on the proxy
         *            instance.
         * @return the value to return from the method invocation on the
         *         proxy instance.
         * @throws Throwable
         *             the exception to throw from the method
         *             invocation on the proxy instance.
         * @see InvocationHandler#invoke(Object, Method, Object[])
         */
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Task task = new Task(method, args);
            switch (m_type) {
                case AsyncUI:
                    m_base.runOnUiThread(task);
                    break;
                case SeparatedThread:
                    new Thread(task).start();
                    break;
                case Direct:
                default:
                    task.run();
                    break;
            }

            return null;
        }

    }

    /**
     * This class implements thread task for listener invocation.
     */
    private class Task implements Runnable {

        private final Method m_method;

        private final Object[] m_args;

        /**
         * Constructor
         * 
         * @param method
         *            called method
         * @param args
         *            method parameters
         */
        public Task(final Method method, final Object[] args) {
            super();
            m_method = method;
            m_args = args;
        }

        /**
         * 
         * @see java.lang.Runnable#run()
         */
        public synchronized void run() {
            directInvoke(m_method, m_args);
        }

        /**
         * Direct invoke of the action.
         * 
         * @param method
         *            called method
         * @param args
         *            method parameters
         */
        protected void directInvoke(final Method method, final Object[] args) {
            try {
                method.invoke(m_target, args);
            } catch (final Throwable ex) {
                LCTX.e("Invokation error: " + method.getName(), ex);
            }
        }
    }
}
