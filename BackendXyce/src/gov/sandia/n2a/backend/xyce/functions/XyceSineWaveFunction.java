/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.backend.xyce.functions;

import gov.sandia.n2a.language.Function;
import gov.sandia.n2a.language.Operator;
import gov.sandia.n2a.language.Type;
import gov.sandia.n2a.language.type.Instance;
import gov.sandia.n2a.language.type.Scalar;

public class XyceSineWaveFunction extends Function
{
    public static Factory factory ()
    {
        return new Factory ()
        {
            public String name ()
            {
                return "sinewave";
            }

            public Operator createInstance ()
            {
                return new XyceSineWaveFunction ();
            }
        };
    }

    public Type eval (Instance context)
    {
        // TODO: implement equivalent of Xyce sinewave function
        return new Scalar (0);
    }

    public String toString ()
    {
        return "sinewave";
    }
}
