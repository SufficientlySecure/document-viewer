package org.ebookdroid.core.actions;


/**
 * This interface defines base features for component controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public interface IActionController<ManagedComponent> {

    /**
     * Name of action property containing managed component
     */
    String MANAGED_COMPONENT_PROPERTY = "ManagedComponent";

    /**
     * Name of action property containing component controller
     */
    String COMPONENT_CONTROLLER_PROPERTY = "ComponentController";

    /**
     * @return the parent controller
     */
    IActionController<?> getParent();

    /**
     * @return the managed component
     */
    ManagedComponent getManagedComponent();

    /**
     * @return the action dispatcher
     */
    ActionDispatcher getDispatcher();

    /**
     * Searches for a global action by the given action id.
     * 
     * @param id
     *            action id
     * @return an instance of the {@link ActionEx} object or <code>null</code>
     */
    ActionEx getAction(final String id);

    /**
     * Searches for a global action by the given action category.
     * 
     * @param category
     *            action category
     * @return an instance of the {@link ActionEx} object or <code>null</code>
     */
    ActionEx getActionByCategory(final String category);

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object or <code>null</code>
     */
    ActionEx getOrCreateAction(final String id);

    /**
     * Creates and register a global action
     * 
     * @param category
     *            action category
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object
     */
    ActionEx getOrCreateAction(final String category, final String id);

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @param parameters
     *            action parameters
     * @return an instance of {@link ActionEx} object
     */
    ActionEx createAction(final String id, final IActionParameter... parameters);

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
     */
    ActionEx createAction(final String category, final String id, final IActionParameter... parameters);

}
