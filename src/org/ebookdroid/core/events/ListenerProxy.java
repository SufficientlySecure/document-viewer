package org.ebookdroid.core.events;

import org.ebookdroid.utils.LengthUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ListenerProxy {

  /**
   * Real listeners.
   */
  private final Map<Class<?>, List<Object>> realListeners = new HashMap<Class<?>, List<Object>>();

  /**
   * Supported interfaces.
   */
  private final Class<?>[] interfaces;

  /**
   * Proxy object.
   */
  private final Object proxy;

  /**
   * Constructor.
   *
   * @param listenerInterfaces
   *          a list of listener interfaces to implement
   */
  public ListenerProxy(final Class<?>... listenerInterfaces) {
    if (LengthUtils.isEmpty(listenerInterfaces)) {
      throw new IllegalArgumentException("Listeners list cannot be empty");
    }

    for (final Class<?> listener : listenerInterfaces) {
      if (listener == null) {
        throw new IllegalArgumentException("Listener class cannot be null");
      }
      if (!listener.isInterface()) {
        throw new IllegalArgumentException("Listener class should be an interface");
      }
    }

    interfaces = listenerInterfaces;

    proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), interfaces, new Handler());
  }

  /**
   * Adds the target listener.
   *
   * @param listener
   *          the listener to add
   */
  public void addListener(final Object listener) {
    if (listener != null) {
      for (final Class<?> listenerClass : interfaces) {
        if (listenerClass.isInstance(listener)) {
          List<Object> list = realListeners.get(listenerClass);
          if (list == null) {
            list = new LinkedList<Object>();
            realListeners.put(listenerClass, list);
          }

          if (!list.contains(listener)) {
            list.add(listener);
          }
        }
      }
    }
  }

  /**
   * Removes the target listener.
   *
   * @param listener
   *          the listener to remove
   */
  public void removeListener(final Object listener) {
    if (listener != null) {
      for (final Class<?> listenerClass : interfaces) {
        if (listenerClass.isInstance(listener)) {
          final List<Object> list = realListeners.get(listenerClass);
          if (list != null) {
            list.remove(listener);
          }
        }
      }
    }
  }

  /**
   * Removes the all target listeners.
   */
  public void removeAllListeners() {
    for (final List<Object> list : realListeners.values()) {
      list.clear();
    }
    realListeners.clear();
  }

  /**
   * Gets the proxy listener casted to the given listener type.
   *
   * @param <Listener>
   *          listener type
   * @return an instance of the <code>Listener</code> type
   */
  @SuppressWarnings("unchecked")
  public <Listener> Listener getListener() {
    return (Listener) proxy;
  }

  /**
   * This class implements invocation handler.
   */
  private class Handler implements InvocationHandler {

    /**
     * Processes a method invocation on a proxy instance and returns the result.
     *
     * @param proxy
     *          the proxy instance that the method was invoked on
     * @param method
     *          the <code>Method</code> instance corresponding to the interface method invoked on the proxy instance.
     * @param args
     *          an array of objects containing the values of the arguments passed in the method invocation on the proxy
     *          instance.
     * @return the value to return from the method invocation on the proxy instance.
     * @throws Throwable
     *           the exception to throw from the method invocation on the proxy instance.
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      final Class<?> listenerClass = method.getDeclaringClass();
      final List<Object> list = realListeners.get(listenerClass);

      if (LengthUtils.isNotEmpty(list)) {
        for (final Object listener : list) {
          method.invoke(listener, args);
        }
      }

      return null;
    }
  }
}
