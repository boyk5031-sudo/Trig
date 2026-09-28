package com.trigger.automation.engine;

import android.view.InputEvent;

interface IInputEventService {
    boolean injectEvent(in InputEvent event, int mode);
    void destroy();
}
