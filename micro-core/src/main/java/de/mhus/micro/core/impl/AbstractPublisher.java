package de.mhus.micro.core.impl;

import de.mhus.lib.core.MLog;
import de.mhus.micro.core.api.MicroPublisher;

public abstract class AbstractPublisher extends MLog implements MicroPublisher {

    protected AbstractApi api;

    public void doInit(AbstractApi api) {
        this.api = api;
    }

	public void doDestroy() {
		
	}

}
