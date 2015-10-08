package gov.sandia.n2a.backend.internal;

import java.util.List;

import gov.sandia.n2a.language.type.Instance;

public class EventSpikeMulti extends EventSpike
{
    List<Instance> targets;

    public void run (Euler simulator)
    {
        System.out.println ("EventSpikeMultiLatch " + t);
        // Note: targets could be null, but in practice that should never happen. The event should not have been created in that case.
        setFlag ();
        for (Instance i : targets) simulator.integrate (i);
        for (Instance i : targets) i.update (simulator);
        for (Instance i : targets) if (! i.finish (simulator)) i.dequeue ();
    }

    public void setFlag ()
    {
        for (Instance i : targets) i.valuesFloat[eventType.valueIndex] = (float) ((int) i.valuesFloat[eventType.valueIndex] | eventType.mask);
    }
}