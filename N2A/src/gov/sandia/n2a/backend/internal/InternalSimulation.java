/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.backend.internal;

import gov.sandia.n2a.eqset.EquationSet;
import gov.sandia.n2a.eqset.Variable;
import gov.sandia.umf.platform.ensemble.params.groupset.ParameterSpecGroupSet;
import gov.sandia.umf.platform.execenvs.ExecutionEnv;
import gov.sandia.umf.platform.plugins.RunOrient;
import gov.sandia.umf.platform.plugins.RunState;
import gov.sandia.umf.platform.plugins.Simulation;
import gov.sandia.umf.platform.ui.ensemble.domains.Parameter;
import gov.sandia.umf.platform.ui.ensemble.domains.ParameterDomain;

import java.io.PrintStream;
import java.io.File;
import java.util.Map.Entry;
import java.util.TreeMap;

public class InternalSimulation implements Simulation
{
    public TreeMap<String,String> metadata = new TreeMap<String, String> ();
    public InternalRunState runState;

    @Override
    public ParameterDomain getAllParameters ()
    {
        return null;
    }

    @Override
    public void setSelectedParameters (ParameterDomain domain)
    {
        for (Entry<Object, Parameter> p : domain.getParameterMap ().entrySet ())
        {
            String name  = p.getKey ().toString ();
            String value = p.getValue ().getDefaultValue ().toString ();
            metadata.put (name, value);
        }
    }

    @Override
    public void submit () throws Exception
    {
        Runnable run = new Runnable ()
        {
            public void run ()
            {
                Wrapper wrapper = null;
                try
                {
                    wrapper = new Wrapper (runState.digestedModel);
                    wrapper.out = new PrintStream (new File (runState.jobDir, "out"));
                    wrapper.err = new PrintStream (new File (runState.jobDir, "err"));

                    Euler simulator = new Euler ();
                    simulator.wrapper = wrapper;
                    simulator.enqueue (wrapper);
                    wrapper.init (simulator);
                    simulator.run ();
                }
                catch (Exception e)
                {
                    if (wrapper != null  &&  wrapper.err != null)
                    {
                        wrapper.err.println (e);
                        e.printStackTrace (wrapper.err);
                    }
                    else
                    {
                        System.err.println (e);
                        e.printStackTrace (System.err);
                    }
                }
            }
        };
        new Thread (run).start ();
    }

    @Override
    public boolean resourcesAvailable()
    {
        return true;
    }

    @Override
    public RunState prepare (Object run, ParameterSpecGroupSet groups, ExecutionEnv env) throws Exception
    {
        // from prepare method
        runState = new InternalRunState ();
        runState.model = ((RunOrient) run).getModel ();

        // Create file for final model
        runState.jobDir = env.createJobDir ();
        String sourceFileName = env.file (runState.jobDir, "model");

        EquationSet e = new EquationSet (runState.model);
        if (e.name.length () < 1) e.name = "Model";  // because the default is for top-level equation set to be anonymous

        // TODO: fix run ensembles to put metadata directly in a special derived part
        e.metadata.putAll (metadata);  // parameters pushed by run system override any we already have

        e.flatten ();
        e.addSpecials ();  // $dt, $index, $init, $live, $n, $t, $type
        e.fillIntegratedVariables ();
        e.findIntegrated ();
        e.resolveLHS ();
        e.resolveRHS ();
        e.findConstants ();
        e.removeUnused ();  // especially get rid of unneeded $variables created by addSpecials()
        e.collectSplits ();
        e.findAccountableConnections ();
        e.findTemporary ();
        e.determineOrder ();
        e.findDerivative ();
        e.addAttribute ("global",      0, false, new String[] {"$max", "$min", "$k", "$n", "$radius"});
        e.addAttribute ("preexistent", 0, true,  new String[] {"$dt", "$t"});
        e.addAttribute ("readOnly",    0, true,  new String[] {"$t"});
        // We don't really need the "simulator" attribute, because it has no impact on the behavior of Internal
        e.replaceConstantWithInitOnly ();
        e.findInitOnly ();
        e.findDeath ();
        e.setAttributesLive ();
        e.setFunctions ();
        e.determineTypes ();

        env.setFileContents (sourceFileName, e.flatList (false));

        createBackendData (e);
        analyze (e);
        clearVariables (e);
        runState.digestedModel = e;

        return runState;
    }

    @Override
    public RunState execute (Object run, ParameterSpecGroupSet groups, ExecutionEnv env) throws Exception
    {
        RunState result = prepare (run, groups, env);
        submit ();
        return result;
    }

    public void createBackendData (EquationSet s)
    {
        if (! (s.backendData instanceof InternalBackendData)) s.backendData = new InternalBackendData ();
        for (EquationSet p : s.parts) createBackendData (p);
    }

    public void analyze (EquationSet s)
    {
        ((InternalBackendData) s.backendData).analyze (s);
        for (EquationSet p : s.parts) analyze (p);
    }

    public void clearVariables (EquationSet s)
    {
        for (EquationSet p : s.parts) clearVariables (p);
        for (Variable v : s.variables) v.type = v.type.clear ();  // So we can use these as backup when stored value is null.
    }
}