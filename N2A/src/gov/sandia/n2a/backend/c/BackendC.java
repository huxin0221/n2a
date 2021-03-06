/*
Copyright 2013-2018 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.backend.c;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import gov.sandia.n2a.backend.internal.InternalBackend;
import gov.sandia.n2a.db.MNode;
import gov.sandia.n2a.eqset.EquationSet;
import gov.sandia.n2a.execenvs.HostSystem;
import gov.sandia.n2a.parms.Parameter;
import gov.sandia.n2a.parms.ParameterDomain;
import gov.sandia.n2a.plugins.extpoints.Backend;

public class BackendC extends Backend
{
    @Override
    public String getName ()
    {
        return "C";
    }

    @Override
    public ParameterDomain getSimulatorParameters ()
    {
        ParameterDomain result = new ParameterDomain ();
        result.addParameter (new Parameter ("duration",     "1.0"  ));  // default is 1 second
        result.addParameter (new Parameter ("c.integrator", "Euler"));  // alt is "RungeKutta"
        return result;
    }

    @Override
    public ParameterDomain getOutputVariables (MNode model)
    {
        try
        {
            if (model == null) return null;
            EquationSet s = new EquationSet (model);
            if (s.name.length () < 1) s.name = "Model";
            s.resolveLHS ();
            return s.getOutputParameters ();
        }
        catch (Exception error)
        {
            return null;
        }
    }

    @Override
    public void start (final MNode job)
    {
        Thread t = new JobC (job);
        t.setDaemon (true);
        t.start ();
    }

    @Override
    public void kill (MNode job)
    {
        long pid = job.getOrDefaultLong ("$metadata", "pid", "0");
        if (pid != 0)
        {
            try
            {
                HostSystem.get (job.getOrDefault ("$metadata", "host", "localhost")).killJob (pid);
                String jobDir = new File (job.get ()).getParent ();
                Files.copy (new ByteArrayInputStream ("killed".getBytes ("UTF-8")), Paths.get (jobDir, "finished"));
            }
            catch (Exception e) {}
        }
    }

    @Override
    public double currentSimTime (MNode job)
    {
        return InternalBackend.getSimTimeFromOutput (job);
    }
}
