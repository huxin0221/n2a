/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.language;

import gov.sandia.n2a.eqset.EquationEntry;
import gov.sandia.n2a.eqset.Variable;
import gov.sandia.n2a.eqset.VariableReference;
import gov.sandia.n2a.language.parse.SimpleNode;
import gov.sandia.n2a.language.type.Instance;

public class AccessVariable extends Operator
{
    public String name; // only needed to resolve the variable (since we will abandon the AST node)
    public VariableReference reference;  // non-null when this node has been resolved in the context of an EquationSet

    public int getOrder ()
    {
        String temp = name;
        int order = 0;
        while (temp.endsWith ("'"))
        {
            order++;
            temp = temp.substring (0, temp.length () - 1);
        }
        return order;
    }

    public String getName ()
    {
        String[] pieces = name.split ("'", 2);
        return pieces[0];
    }

    public void getOperandsFrom (SimpleNode node)
    {
        name = node.jjtGetValue ().toString ();
    }

    public Operator simplify (Variable from)
    {
        if (reference == null  ||  reference.variable == null) return this;  // unresolved!
        Variable v = reference.variable;
        if (v.name.equals ("$init")) return this;  // specifically prevent $init from being replaced by a Constant
        if (v.hasAttribute ("externalWrite")) return this;  // A variable may locally evaluate to a constant, yet be subject to change from outside equations.
        if (v.equations.size () != 1) return this;
        EquationEntry e = v.equations.first ();
        if (e.expression == null  ||  e.conditional != null) return this;
        if (e.expression instanceof Constant) return e.expression;

        // Attempt to simplify expression, and maybe get a Constant
        Variable p = from;
        while (p != null)
        {
            if (p == v) return this;  // can't simplify, because we've already visited this variable
            p = p.visited;
        }
        v.visited = from;
        e.expression = e.expression.simplify (v);
        if (e.expression instanceof Constant) return e.expression;
        return this;
    }

    public Type eval (Instance instance)
    {
        return instance.get (reference);
    }

    public String toString ()
    {
        return name;
    }

    public int compareTo (Operator that)
    {
        Class<? extends Operator> thisClass = getClass ();
        Class<? extends Operator> thatClass = that.getClass ();
        if (! thisClass.equals (thatClass)) return thisClass.hashCode () - thatClass.hashCode ();

        // Same class as us, so compare operands
        AccessVariable a = (AccessVariable) that;
        return name.compareTo (a.name);
    }
}
