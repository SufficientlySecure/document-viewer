package org.ebookdroid.core.actions;

import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.utils.LengthUtils;

import android.app.Activity;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class defines base features for action controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public abstract class AbstractComponentController<ManagedComponent> implements IActionController<ManagedComponent> {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("Actions");

    private final HashMap<String, ActionEx> m_actions = new HashMap<String, ActionEx>();

    private final HashMap<String, ActionEx> m_categoryActions = new HashMap<String, ActionEx>();

    private final ReentrantReadWriteLock m_actionsLock = new ReentrantReadWriteLock();

    private final ManagedComponent m_managedComponent;

    private final IActionController<?> m_parent;

    private final ActionDispatcher m_dispatcher;

    /**
     * Constructor
     * 
     * @param managedComponent
     *            managed component
     */
    protected AbstractComponentController(final Activity base, final ManagedComponent managedComponent) {
        this(base, null, managedComponent);
    }

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent controller
     * @param managedComponent
     *            managed component
     */
    protected AbstractComponentController(final Activity base, final IActionController<?> parent,
            final ManagedComponent managedComponent) {
        m_parent = parent;
        m_managedComponent = managedComponent;
        m_dispatcher = new ActionDispatcher(base, this);
    }

    /**
     * @return the parent controller
     * @see IActionController#getParent()
     */
    public IActionController<?> getParent() {
        return m_parent;
    }

    /**
     * @return the managed component
     * @see IActionController#getManagedComponent()
     */
    public ManagedComponent getManagedComponent() {
        return m_managedComponent;
    }

    /**
     * @return the action dispatcher
     * @see IActionController#getDispatcher()
     */
    public ActionDispatcher getDispatcher() {
        return m_dispatcher;
    }

    /**
     * Searches for an action by the given id
     * 
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object or <code>null</code>
     * @see IActionController#getAction(java.lang.String)
     */
    public ActionEx getAction(final String id) {
        m_actionsLock.readLock().lock();
        try {
            ActionEx actionEx = m_actions.get(id);
            if (actionEx == null && m_parent != null) {
                actionEx = m_parent.getAction(id);
            }
            return actionEx;
        } finally {
            m_actionsLock.readLock().unlock();
        }
    }

    /**
     * Searches for a global action by the given action category.
     * 
     * @param category
     *            action category
     * @return an instance of the {@link ActionEx} object or <code>null</code>
     * @see IActionController#getActionByCategory(java.lang.String)
     */
    public ActionEx getActionByCategory(final String category) {
        m_actionsLock.readLock().lock();
        try {
            return m_categoryActions.get(category);
        } finally {
            m_actionsLock.readLock().unlock();
        }
    }

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object or <code>null</code>
     */
    public ActionEx getOrCreateAction(final String id) {
        return getOrCreateAction(null, id);
    }

    /**
     * Creates and register a global action
     * 
     * @param category
     *            action category
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object
     * @see IActionController#getOrCreateAction(String, String, IActionParameter[])
     */
    public ActionEx getOrCreateAction(final String category, final String id) {
        ActionEx result = null;
        m_actionsLock.writeLock().lock();
        try {
            result = getAction(id);
            if (result == null) {
                result = createAction(category, id);
                try {
                    ActionInitControllerMethod.getInstance().invoke(this, result);
                } catch (Throwable e) {
                    LCTX.e("Action " + id + "initialization failed: ", e);
                }

                m_actions.put(result.getId(), result);
                if (LengthUtils.isNotEmpty(category)) {
                    m_categoryActions.put(category, result);
                }
            }
        } finally {
            m_actionsLock.writeLock().unlock();
        }

        return result;
    }

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @param parameters
     *            action parameters
     * @return an instance of {@link ActionEx} object
     * @see IActionController#createAction(String, IActionParameter[])
     */
    public ActionEx createAction(final String id, final IActionParameter... parameters) {
        return createAction(null, id, parameters);
    }

    /**
     * Creates an action
     * 
     * @param category
     *            action category
     * @param id
     *            action id
     * @param parameters
     *            action parameters
     * @return an instance of {@link ActionEx} object
     * @see IActionController#createAction(String, String, IActionParameter[])
     */
    public ActionEx createAction(final String category, final String id, final IActionParameter... parameters) {
        ActionEx result = new ActionEx(this, category, id);

        result.putValue(MANAGED_COMPONENT_PROPERTY, getManagedComponent());
        result.putValue(COMPONENT_CONTROLLER_PROPERTY, this);

        for (IActionParameter actionParameter : parameters) {
            result.addParameter(actionParameter);
        }

        try {
            ActionInitControllerMethod.getInstance().invoke(this, result);
        } catch (Throwable e) {
            LCTX.e("Action " + id + "initialization failed: ", e);
        }

        m_actionsLock.writeLock().lock();
        try {
            m_actions.put(result.getId(), result);
            if (LengthUtils.isNotEmpty(category)) {
                m_categoryActions.put(category, result);
            }
        } finally {
            m_actionsLock.writeLock().unlock();
        }

        return result;
    }
}
