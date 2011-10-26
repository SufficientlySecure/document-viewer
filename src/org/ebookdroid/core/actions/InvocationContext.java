package org.ebookdroid.core.actions;

public @interface InvocationContext {

    InvokationType name() default InvokationType.Direct;
}
