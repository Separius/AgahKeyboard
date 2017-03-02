package io.separ.neural.inputmethod.slash;

import com.android.inputmethod.keyboard.top.services.ServiceResultsView;

/**
 * Created by sepehr on 3/2/17.
 */
public class ServiceRequestEvent {
    public String service;
    private ServiceResultsView.VisualSate state;

    public ServiceRequestEvent(ServiceResultsView.VisualSate state, String service) {
        this.state = state;
        this.service = service;
    }

    public ServiceResultsView.VisualSate getState() {
        return this.state;
    }
}
