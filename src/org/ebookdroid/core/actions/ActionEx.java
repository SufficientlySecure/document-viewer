package org.ebookdroid.core.actions;

import org.ebookdroid.core.log.LogContext;

import android.content.DialogInterface;
import android.drm.DrmStore.Action;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ActionEx implements Runnable, View.OnClickListener, DialogInterface.OnClickListener {

    private static final LogContext LOGGER = LogContext.ROOT.lctx("Actions");

    private static final String SHORT_DESCRIPTION = "ShortDescription";

    private int m_id;

    private String m_text;

    private ActionControllerMethod m_method;

    private Map<String, Object> m_values = new LinkedHashMap<String, Object>();

    private Map<String, IActionParameter> m_actionParameters = new LinkedHashMap<String, IActionParameter>();

    /**
     * Constructor
     *
     * @param controller
     *            action controller
     * @param category
     *            action category
     * @param id
     *            action id
     */
    ActionEx(final IActionController<?> controller, final int id) {
        m_id = id;
        m_method = new ActionControllerMethod(controller, id);
    }

    /**
     * Returns the action's id.
     *
     * @return the id
     */
    public int getId() {
        return m_id;
    }

    /**
     * Returns the action's text.
     *
     * @return the actions text
     * @see #setName
     */
    public String getText() {
        return m_text;
    }

    /**
     * Returns the action's description.
     *
     * @return the actions description
     * @see #setDescription
     */
    public String getDescription() {
        return getParameter(SHORT_DESCRIPTION);
    }

    /**
     * Sets the action's description.
     *
     * @param description
     *            the string used to set the action's description
     * @see #getDescription
     * @beaninfo bound: true preferred: true attribute: visualUpdate true
     *           description: The action's name.
     */
    public void setDescription(final String description) {
        putValue(SHORT_DESCRIPTION, description);
    }

    /**
     * @return ActionControllerMethod
     */
    public ActionControllerMethod getMethod() {
        return m_method;
    }

    /**
     * Returns component managed by action controller created this action.
     *
     * @param <ManagedComponent>
     *            managed component type
     * @return component managed by action controller created this action
     */
    public <ManagedComponent> ManagedComponent getManagedComponent() {
        return getParameter(IActionController.MANAGED_COMPONENT_PROPERTY);
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     *
     * @param key
     *            a string containing the specified <code>key</code>
     * @return the binding <code>Object</code> stored with this key; if
     *         there are no keys, it will return <code>null</code>
     * @see javax.swing.AbstractAction#getValue(java.lang.String)
     */
    @Deprecated
    public Object getValue(final String key) {
        return m_values.get(key);
    }

    public ActionEx putValue(final String key, final Object value) {
        m_values.put(key, value);
        return this;
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     *
     * @param <T>
     *            parameter type
     * @param key
     *            a string containing the specified <code>key</code>
     * @return the binding <code>Object</code> stored with this key; if
     *         there are no keys, it will return defaultValue parameter
     * @see Action#getValue(String)
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(final String key) {
        return (T) m_values.get(key);
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     *
     * @param <T>
     *            parameter type
     * @param key
     *            a string containing the specified <code>key</code>
     * @param defaultValue
     *            default value
     * @return the binding <code>Object</code> stored with this key; if
     *         there are no keys, it will return defaultValue parameter
     * @see Action#getValue(String)
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(final String key, final T defaultValue) {
        Object value = getParameter(key);
        return (T) (value != null ? value : defaultValue);
    }

    /**
     * Adds a parameter to the action
     *
     * @param parameter
     *            action parameter to set
     */
    public ActionEx addParameter(final IActionParameter parameter) {
        m_actionParameters.put(parameter.getName(), parameter);
        return this;
    }

    @Override
    public void run() {
        ActionControllerMethod method = getMethod();
        try {
            setParameters();
            method.invoke(this);
        } catch (Throwable th) {
            LOGGER.e("Action " + getId() + " failed on execution: ", th);
        } finally {
        }
    }

    @Override
    public void onClick(View view) {
        if (getMethod().isValid()) {
            this.putValue(IActionController.VIEW_PROPERTY, view);
            run();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (getMethod().isValid()) {
            this.putValue(IActionController.DIALOG_PROPERTY, dialog);
            this.putValue(IActionController.DIALOG_ITEM_PROPERTY, which);
            this.run();
        }
    }

    /**
     * Sets parameter values to the action
     */
    private void setParameters() {
        for (Entry<String, IActionParameter> entry : m_actionParameters.entrySet()) {
            putValue(entry.getKey(), entry.getValue().getValue());
        }
    }

}
