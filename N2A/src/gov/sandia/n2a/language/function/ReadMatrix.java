/*
Copyright 2013-2017 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.language.function;

import java.io.File;

import gov.sandia.n2a.backend.internal.Simulator;
import gov.sandia.n2a.language.Function;
import gov.sandia.n2a.language.Operator;
import gov.sandia.n2a.language.Type;
import gov.sandia.n2a.language.type.Instance;
import gov.sandia.n2a.language.type.Matrix;
import gov.sandia.n2a.language.type.Scalar;
import gov.sandia.n2a.language.type.Text;

public class ReadMatrix extends Function
{
    public static Factory factory ()
    {
        return new Factory ()
        {
            public String name ()
            {
                return "matrix";
            }

            public Operator createInstance ()
            {
                return new ReadMatrix ();
            }
        };
    }

    public boolean canBeConstant ()
    {
        return false;
    }

    public boolean canBeInitOnly ()
    {
        return true;
    }

    public Type eval (Instance context)
    {
        Simulator simulator = Simulator.getSimulator (context);
        if (simulator == null) return new Scalar (0);  // absence of simulator indicates analysis phase, so opening files is unnecessary

        String path = ((Text) operands[0].eval (context)).value;
        Matrix A = simulator.matrices.get (path);
        if (A == null)
        {
            A = Matrix.factory (new File (path).getAbsoluteFile ());
            simulator.matrices.put (path, A);
        }

        String mode = "";
        int lastParm = operands.length - 1;
        if (lastParm > 0)
        {
            Type parmValue = operands[lastParm].eval (context);
            if (parmValue instanceof Text) mode = ((Text) parmValue).value;
        }
        if (mode.equals ("columns")) return new Scalar (A.columns ());
        if (mode.equals ("rows"   )) return new Scalar (A.rows    ());

        double row    = ((Scalar) operands[1].eval (context)).value;
        double column = ((Scalar) operands[2].eval (context)).value;
        return new Scalar (A.get (row, column, mode.equals ("raw")));
    }

    public String toString ()
    {
        return "matrix";
    }
}
