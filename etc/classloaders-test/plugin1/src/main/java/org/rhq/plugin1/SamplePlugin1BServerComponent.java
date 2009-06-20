package org.rhq.plugin1;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import dummy.Dummy;

public class SamplePlugin1BServerComponent implements ResourceComponent {
    private ResourceContext context;

    public void start(ResourceContext context) {
        this.context = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        System.out.println("+ plugin1-1B avail Dummy.VERSION=" + Dummy.VERSION);
        System.out.println("+ plugin1-1B avail Dummy.getVersion=" + (new Dummy()).getVersion());
        System.out.println("+ plugin1-1B avail Dummy classloader=" + Dummy.class.getClassLoader());
        System.out.println("+ plugin1-1B avail resourcetype=" + this.context.getResourceType());
        System.out.println("+ plugin1-1B avail this classloader=" + this.getClass().getClassLoader());
        System.out.println("+ plugin1-1B avail ctx classloader=" + Thread.currentThread().getContextClassLoader());
        System.out.println("==========");
        return AvailabilityType.UP;
    }
}
