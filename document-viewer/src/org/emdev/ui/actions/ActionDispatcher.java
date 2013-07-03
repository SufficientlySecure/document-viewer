package org.emdev.ui.actions;


import android.app.Activity;

import java.util.concurrent.ThreadPoolExecutor;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public class ActionDispatcher {

    /**
     * Name for additional action parameters.
     */
    public static final String PARAMETERS = "Parameters";

    private static final LogContext LCTX = LogManager.root().lctx("Actions");

    final Activity m_base;

    final IActionController<?> m_controller;

    final ThreadPoolExecutor m_pool;

    /**
     * Constructor
     *
     * @param controller
     *            owner controller
     */
    public ActionDispatcher(final Activity base, final IActionController<?> controller) {
        this(base, controller, null);
    }

    /**
     * Constructor
     *
     * @param controller
     *            owner controller
     * @param pool
     *            thread pool
     */
    public ActionDispatcher(final Activity base, final IActionController<?> controller, final ThreadPoolExecutor pool) {
        m_base = base;
        m_controller = controller;
        m_pool = pool;
    }

    /**
     * Invoke action.
     *
     * @param type
     *            the invokation type
     * @param actionId
     *            the action id
     * @param parameters
     *            action parameters
     */
    public void invoke(final InvokationType type, final int actionId, final Object... parameters) {
        ActionEx action = new ActionEx(m_controller, actionId);
        action.putValue(PARAMETERS, parameters);

        invoke(type, action);
    }

    /**
     * Invoke action.
     *
     * @param type
     *            the invokation type
     * @param action
     *            action to run
     */
    private void invoke(final InvokationType type, final ActionEx action) {
        ActionControllerMethod method = action.getMethod();
        if (!method.isValid()) {
            LCTX.e("The action method for action "  + action.name + " is not valid: ", method.getErrorInfo());
            return;
        }

        switch (type) {
            case AsyncUI:
                m_base.runOnUiThread(action);
                break;
            case SeparatedThread:
                if (m_pool != null) {
                    m_pool.execute(action);
                } else {
                    new Thread(action).start();
                }
                break;
            case Direct:
            default:
                action.run();
                break;
        }
    }
}
