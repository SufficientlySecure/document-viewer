package org.ebookdroid.core.actions;

import android.app.Activity;

/**
 * This class defines base features for action controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public class ActionController<ManagedComponent> extends AbstractComponentController<ManagedComponent> {

    /**
     * Constructor
     * 
     * @param managedComponent
     *            managed component
     */
    public ActionController(final Activity base, final ManagedComponent managedComponent) {
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
    public ActionController(final Activity base, final IActionController<?> parent, final ManagedComponent managedComponent) {
        super(base, parent, managedComponent);
    }
}
