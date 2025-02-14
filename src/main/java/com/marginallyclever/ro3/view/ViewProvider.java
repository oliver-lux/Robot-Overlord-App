package com.marginallyclever.ro3.view;

/**
 * <p>Classes implementing {@link ViewProvider} are saying that they can provide Swing components that can be used
 * to view or manipulate a subject instance.  Each {@link ViewProvider} class should provide a default constructor so
 * that the components can be inspected without providing an instance of the subject.</p>
 * @param <T> The type of object to be viewed or manipulated.
 */
public interface ViewProvider<T> {
    /**
     * Set the subject of this ViewProvider.
     * @param subject The subject to be viewed or manipulated.
     */
    void setViewSubject(T subject);
}
