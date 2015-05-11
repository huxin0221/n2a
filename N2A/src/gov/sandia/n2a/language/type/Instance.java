/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.language.type;

import java.util.HashMap;
import java.util.Map;

import gov.sandia.n2a.eqset.EquationEntry;
import gov.sandia.n2a.eqset.EquationSet;
import gov.sandia.n2a.eqset.Variable;
import gov.sandia.n2a.language.EvaluationException;
import gov.sandia.n2a.language.Type;

/**
    Instance type. Represents a entire concrete object at simulation time.
**/
public class Instance extends Type
{
    public EquationSet part;
    public Map<Variable, Type> values = new HashMap<Variable, Type> ();
    public Instance container;

    public Instance ()
    {
    }

    public Instance (EquationSet part)
    {
        this.part = part;
    }

    public Type get (Variable v) throws EvaluationException
    {
        if (values.containsKey (v)) return values.get (v);
        values.put (v, null);  // If successful, we will replace this with the correct result. Otherwise, it remains null. This also guards against infinite recursion.

        boolean type = v.name.equals ("$type");  // requires special (complex) handling

        // Select the default equation
        boolean init = v.container.getInit ();
        EquationEntry defaultEquation = null;
        for (EquationEntry e : v.equations)
        {
            if (init  &&  e.ifString.equals ("$init"))  // TODO: also handle $init==1, or any other equivalent expression
            {
                defaultEquation = e;
                break;
            }
            if (e.ifString.length () == 0)
            {
                defaultEquation = e;
            }
        }

        boolean evaluated = false;
        Type result = null;
        for (EquationEntry e : v.equations)  // Scan for first equation whose condition is nonzero
        {
            if (e == defaultEquation) continue;
            Object doit = e.conditional.eval (this);
            if (doit instanceof Scalar  &&  ((Scalar) doit).value != 0)
            {
                if (type) result = new Scalar (0);  // TODO: process $type split (when we create an internal N2A interpreter)
                else      result = (Type) e.expression.eval (this);
                evaluated = true;
            }
        }
        if (! evaluated  &&  defaultEquation != null)
        {
            if (type) result = new Scalar (0);  // TODO: process $type split (when we create an internal N2A interpreter)
            else      result = (Type) defaultEquation.expression.eval (this);
        }
        if (result != null)
        {
            values.put (v, result); // Note: If we fail to compute a new value, and the variable already had one, it will retain the old value.
            return result;
        }
        return values.get (v);
    }

    public void set (Variable v, Type value)
    {
        values.put (v, value);
    }

    public void remove (Variable v)
    {
        values.remove (v);
    }

    public Type EQ (Type that) throws EvaluationException
    {
        if (this == that) return new Scalar (1);
        return new Scalar (0);
    }

    public Type NE (Type that) throws EvaluationException
    {
        if (this == that) return new Scalar (0);
        return new Scalar (1);
    }

    public Type GT (Type that) throws EvaluationException
    {
        if (! (that instanceof Instance)) throw new EvaluationException ("Type mismatch");
        if (this.hashCode () > that.hashCode ()) return new Scalar (1);
        return new Scalar (0);
    }

    public Type GE (Type that) throws EvaluationException
    {
        if (this == that) return new Scalar (1);
        if (! (that instanceof Instance)) throw new EvaluationException ("Type mismatch");
        if (this.hashCode () > that.hashCode ()) return new Scalar (1);
        return new Scalar (0);
    }

    public Type LT (Type that) throws EvaluationException
    {
        if (! (that instanceof Instance)) throw new EvaluationException ("Type mismatch");
        if (this.hashCode () < that.hashCode ()) return new Scalar (1);
        return new Scalar (0);
    }

    public Type LE (Type that) throws EvaluationException
    {
        if (this == that) return new Scalar (1);
        if (! (that instanceof Instance)) throw new EvaluationException ("Type mismatch");
        if (this.hashCode () < that.hashCode ()) return new Scalar (1);
        return new Scalar (0);
    }

    public String toString ()
    {
        return part.name;
    }
}
