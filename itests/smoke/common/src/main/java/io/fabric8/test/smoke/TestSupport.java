package io.fabric8.test.smoke;

import org.apache.felix.scr.*;
import org.jboss.gravia.runtime.ServiceLocator;

/**
 */
public class TestSupport {

    /**
     * Returns a literal for the {@link org.apache.felix.scr.Component} state.
     * @param component     The target {@link org.apache.felix.scr.Component}.
     * @return
     */
    private static String getState(Component component) {
        switch (component.getState()) {
            case Component.STATE_ACTIVE:
                return "Active";
            case Component.STATE_ACTIVATING:
                return "Activating";
            case Component.STATE_DEACTIVATING:
                return "Deactivating";
            case Component.STATE_DISABLED:
                return "Disabled";
            case Component.STATE_DISABLING:
                return "Disabling";
            case Component.STATE_DISPOSED:
                return "Disposed";
            case Component.STATE_DISPOSING:
                return "Disposing";
            case Component.STATE_ENABLING:
                return "Enabling";
            case Component.STATE_FACTORY:
                return "Factory";
            case Component.STATE_REGISTERED:
                return "Registered";
            case Component.STATE_UNSATISFIED:
                return "Unsatisfied";
        }
        return "Unknown";
    }

    public static void printInactiveScrServices() {
        ScrService scr = ServiceLocator.getRequiredService(ScrService.class);
        if( scr.getComponents()!=null ) {
            System.out.println("== Inactive SCR Components =======================");
            for (Component component : scr.getComponents()) {
                if( component.getState()!=Component.STATE_ACTIVE ) {
                    System.out.println(String.format("%s: %s", component.getName(), getState(component)));
                    if( component.getReferences()!=null ) {
                        for (Reference reference : component.getReferences()) {
                            if( !reference.isSatisfied() ) {
                                System.out.println(String.format("  %s: %s", reference.getName(), reference.isSatisfied()));
                            }
                        }
                    }
                }
            }
            System.out.println("==================================================");
        } else {
            System.out.println("No components registered in SCR");
        }
    }
}
