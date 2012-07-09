package org.emdev.ui.widget;

import android.app.Dialog;
import android.view.View;

public interface IViewContainer {

    View findViewById(int id);

    public abstract class AbstractContainerBridge<T> implements IViewContainer {

        protected final T container;

        private AbstractContainerBridge(T container) {
            this.container = container;
        }
    }

    public class ViewBridge extends AbstractContainerBridge<View> {

        public ViewBridge(View view) {
            super(view);
        }

        @Override
        public View findViewById(int id) {
            return container.findViewById(id);
        }
    }
    
    public class DialogBridge extends AbstractContainerBridge<Dialog> {

        public DialogBridge(Dialog dialog) {
            super(dialog);
        }

        @Override
        public View findViewById(int id) {
            return container.findViewById(id);
        }
    }

}
