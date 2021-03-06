/*
Copyright 2013-2019 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.backend.internal;

import gov.sandia.n2a.language.function.Draw;
import gov.sandia.n2a.language.function.Input;
import gov.sandia.n2a.language.function.Output;
import gov.sandia.n2a.language.type.Instance;
import gov.sandia.n2a.language.type.Matrix;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
    The integrator for the Internal simulator.
    Internal is never meant to become a high-performance simulator, so Euler is all we ever expect to support.
    Space-efficiency is the true priority for Internal, since it supports code-generation for other backends,
    and thus makes the size of their models memory-bound.
**/
public class Simulator implements Iterable<Part>
{
    public Wrapper                     wrapper;  // reference to top-level model, which is also in the simulation queue
    public EventFactory                eventFactory;
    public Queue<Event>                queueEvent    = new PriorityQueue<Event> ();
    public List<ResizeRequest>         queueResize   = new LinkedList<ResizeRequest> ();
    public Queue<Population>           queueConnect  = new ConcurrentLinkedQueue<Population> ();
    public Set<Population>             queueClearNew = new TreeSet<Population> ();
    public TreeMap<Double,EventStep>   periods       = new TreeMap<Double,EventStep> ();
    public Random                      random;

    // Global shared data
    public Path                      jobDir;
    public Map<String,Matrix>        matrices = new HashMap<String,Matrix> ();
    public Map<String,Input .Holder> inputs   = new HashMap<String,Input .Holder> ();
    public Map<String,Output.Holder> outputs  = new HashMap<String,Output.Holder> ();
    public Map<String,Draw  .Holder> drawings = new HashMap<String,Draw  .Holder> ();
    public PrintStream               out;
    // Note: System.in will get bound into an Input.Holder if used at all.

    public static int BEFORE = -1;
    public static int DURING =  0;
    public static int AFTER  =  1;
    public int eventMode = DURING;

    public Event currentEvent;
    public boolean stop;  // Flag to terminate event loop as soon as possible

    public class ResizeRequest
    {
        public Population population;
        public int        size;
        public ResizeRequest (Population population, int size)
        {
            this.population = population;
            this.size       = size;
        }
    }

    public Simulator (Wrapper wrapper, long seed) throws IOException
    {
        this (wrapper, seed, Files.createTempDirectory ("n2a"));
    }

    public Simulator (Wrapper wrapper, long seed, Path jobDir)
    {
        this (wrapper, seed, jobDir, new EventFactory ());
    }

    public Simulator (Wrapper wrapper, long seed, Path jobDir, EventFactory factory)
    {
        this.jobDir = jobDir;
        try {out = new PrintStream (jobDir.resolve ("out").toFile (), "UTF-8");}      // put in current working dir, which should be the job directory
        catch (Exception e) {out = System.out;}  // if that fails, just use the default stdout

        random = new Random (seed);

        this.wrapper = wrapper;
        wrapper.simulator = this;

        eventFactory = factory;
        EventStep e = eventFactory.create (0.0, 1e-4);
        periods.put (e.dt, e);
        currentEvent = e;
    }

    /**
        Perform the init cycle at time zero.
        run() picks up immediately after this to continue the simulation.
        Mainly, this function constructs the network, which is useful for export.
    **/
    public void init ()
    {
        EventStep e = (EventStep) currentEvent;
        e.enqueue (wrapper);
        wrapper.init (this);
        updatePopulations ();

        if (e.head.next != e.head)  // if event is not empty (that is, if model did not move itself to a different period)
        {
            e.t = e.dt;
            queueEvent.add (e);
        }
    }

    public static Simulator getSimulator (Instance context)
    {
        Simulator simulator = null;
        if (context instanceof InstanceTemporaries)
        {
            simulator = ((InstanceTemporaries) context).simulator;
        }
        else
        {
            Instance top = context;
            while (top.container != null) top = top.container;
            if (top instanceof Wrapper) simulator = ((Wrapper) top).simulator;
        }
        return simulator;
    }

    public void run ()
    {
        // This is the core simulation loop.
        while (! queueEvent.isEmpty ()  &&  ! stop)
        {
            currentEvent = queueEvent.remove ();
            currentEvent.run (this);
        }
        // Simulation is done.
        closeStreams ();
    }

    public void closeStreams ()
    {
        for (Entry<String,Input .Holder> h : inputs  .entrySet ()) h.getValue ().close ();
        for (Entry<String,Output.Holder> h : outputs .entrySet ()) h.getValue ().close ();
        for (Entry<String,Draw  .Holder> h : drawings.entrySet ()) h.getValue ().close ();
    }

    public void integrate (Instance i)
    {
        i.integrate (this);
    }

    public void updatePopulations ()
    {
        // Resize populations that have requested it
        for (ResizeRequest r : queueResize) r.population.resize (this, r.size);
        queueResize.clear ();

        // Evaluate connection populations that have requested it
        // To support nested connections, this is structured as an actual queue.
        // Note: The creation of nested connections, or even populations within a connection instance, should not touch the resize queue.
        while (! queueConnect.isEmpty ())
        {
            queueConnect.remove ().connect (this);
        }

        // Clear new flag from populations that have requested it
        for (Population p : queueClearNew) p.clearNew ();
        queueClearNew.clear ();
    }

    public void move (Part i, double dt)
    {
        // find a matching event, or create one
        EventStep e = null;
        Entry<Double,EventStep> result = periods.floorEntry (dt);
        if (result != null) e = result.getValue ();
        if (e == null  ||  e.dt != dt)
        {
            e = eventFactory.create (currentEvent.t + dt, dt);
            periods.put (dt, e);
            queueEvent.add (e);
        }

        // transfer to new event's queue
        i.dequeue ();
        e.enqueue (i);
        //   keep top-level model and wrapper together in same event
        if (i.container == wrapper)
        {
            wrapper.dequeue ();
            e.enqueue (wrapper);
        }
    }

    public double getNextDt ()
    {
        for (Event e : queueEvent)
        {
            if (e instanceof EventStep) return ((EventStep) e).dt;
        }
        return 1e-4;
    }

    public void resize (Population p, int n)
    {
        queueResize.add (new ResizeRequest (p, n));
    }

    public void connect (Population p)
    {
        queueConnect.add (p);
    }

    public void clearNew (Population p)
    {
        queueClearNew.add (p);
    }

    public class PartIterator implements Iterator<Part>
    {
        Iterator<Event> eventIterator;
        EventStep       event;
        Part            part;

        public PartIterator ()
        {
            eventIterator = queueEvent.iterator ();
            nextEvent ();
        }

        public boolean hasNext ()
        {
            if (part == null) return false;
            return part != event.head;
        }

        public Part next ()
        {
            Part result = part;
            part = part.next;
            if (part == event.head) nextEvent ();
            return result;
        }

        public void nextEvent ()
        {
            event = null;
            while (eventIterator.hasNext ())
            {
                Event e = eventIterator.next ();
                if (e instanceof EventStep)
                {
                    event = (EventStep) e;
                    break;
                }
            }
            if (event == null) part = null;
            else               part = event.head.next;
        }

        public void remove ()
        {
            throw new UnsupportedOperationException ();
        }
    }

    public Iterator<Part> iterator ()
    {
        return new PartIterator ();
    }
}
