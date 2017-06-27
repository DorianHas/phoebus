package org.phoebus.core.types.adapters;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.LogEntryFactory;

@ProviderFor(AdapterFactory.class)
public class TimeStampedProcessVariableAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(String.class, LogEntry.class);

    public Class getAdaptableObject() {
        return TimeStampedProcessVariable.class;
    }

    public Optional getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType.isAssignableFrom(LogEntry.class)) {
            TimeStampedProcessVariable tpv = ((TimeStampedProcessVariable) adaptableObject);
            return Optional.of(LogEntryFactory.buildLogEntry("PV name: " + tpv.getName() + " " + tpv.getTime()).create());
        } else if (adapterType.isAssignableFrom(String.class)) {
            TimeStampedProcessVariable tpv = ((TimeStampedProcessVariable) adaptableObject);
            return Optional.of("PV name: " + tpv.getName() + " " + tpv.getTime());
        }
        return Optional.ofNullable(null);
    }

    public List<? extends Class> getAdapterList() {
        return adaptableTypes;
    }

}