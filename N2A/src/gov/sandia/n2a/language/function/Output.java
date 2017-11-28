/*
Copyright 2013-2017 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.language.function;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import gov.sandia.n2a.backend.internal.InstanceTemporaries;
import gov.sandia.n2a.backend.internal.Simulator;
import gov.sandia.n2a.eqset.EquationSet;
import gov.sandia.n2a.eqset.Variable;
import gov.sandia.n2a.language.AccessVariable;
import gov.sandia.n2a.language.Constant;
import gov.sandia.n2a.language.Function;
import gov.sandia.n2a.language.Operator;
import gov.sandia.n2a.language.Type;
import gov.sandia.n2a.language.Visitor;
import gov.sandia.n2a.language.type.Instance;
import gov.sandia.n2a.language.type.Scalar;
import gov.sandia.n2a.language.type.Text;

public class Output extends Function
{
    public String variableName;  // Trace needs to know its target variable in order to auto-generate a column name. This value is set by an analysis process.
    public int    index;  // of column name in valuesObject array

    public static Factory factory ()
    {
        return new Factory ()
        {
            public String name ()
            {
                return "output";
            }

            public Operator createInstance ()
            {
                return new Output ();
            }
        };
    }

    public boolean isOutput ()
    {
        return true;
    }

    public boolean canBeConstant ()
    {
        return false;
    }

    public static class Holder
    {
        public Map<String,Integer> columnMap    = new HashMap<String,Integer> ();  ///< Maps from column name to column position.
        public List<Float>         columnValues = new ArrayList<Float> ();         ///< Holds current value for each column.
        public int                 columnsPrevious;                                ///< Number of columns written in previous cycle.
        public boolean             traceReceived;                                  ///< Indicates that at least one column was touched during the current cycle.
        public double              t;
        public PrintStream         out;
        public Simulator           simulator;  ///< So we can get time associated with each trace() call.
        public boolean             raw;  ///< Indicates that column is an exact index.

        public void trace (String column, float value)
        {
            // Detect when time changes and dump any previously traced values.
            double now;
            if (simulator.currentEvent == null) now = 0;
            else                                now = (float) simulator.currentEvent.t;
            if (now > t)
            {
                writeTrace ();
                t = now;
            }

            if (! traceReceived)  // First trace for this cycle
            {
                if (columnValues.isEmpty ())  // slip $t into first column 
                {
                    columnMap.put ("$t", 0);
                    columnValues.add ((float) t);
                }
                else
                {
                    columnValues.set (0, (float) t);
                }
            }

            Integer index = columnMap.get (column);
            if (index == null)
            {
                if (raw)
                {
                    int i = Integer.valueOf (column) + 1;  // offset for time in first column
                    while (columnValues.size () < i) columnValues.add (Float.NaN);
                    columnMap.put (column, i);
                }
                else
                {
                    columnMap.put (column, columnValues.size ());
                }
                columnValues.add (value);
            }
            else
            {
                columnValues.set (index, value);
            }

            traceReceived = true;
        }

        public void writeTrace ()
        {
            if (! traceReceived) return;  // Don't output anything unless at least one value was set.

            int count = columnValues.size ();
            int last  = count - 1;

            // Write headers if new columns have been added
            if (! raw  &&  count > columnsPrevious)
            {
                String headers[] = new String[count];
                for (Entry<String,Integer> i : columnMap.entrySet ())
                {
                    headers[i.getValue ()] = i.getKey ();
                }
                out.print (headers[0]);  // Should be $t
                int i = 1;
                for (; i < columnsPrevious; i++)
                {
                    out.print ("\t");
                }
                for (; i < count; i++)
                {
                    out.print ("\t");
                    out.print (headers[i]);
                }
                out.println ();
                columnsPrevious = count;
            }

            // Write values
            for (int i = 0; i <= last; i++)
            {
                Float c = columnValues.get (i);
                if (! c.isNaN ()) out.print (c);
                if (i < last) out.print ("\t");
                columnValues.set (i, Float.NaN);
            }
            out.println ();

            traceReceived = false;
        }
    }

    public Type eval (Instance context)
    {
        int columnParameter = 1;
        Type result = operands[0].eval (context);  // This will be either the expression itself, or the destination file, depending on form.
        String path = "";
        if (result instanceof Text)
        {
            path = ((Text) result).value;
            result = operands[1].eval (context);  // If the first operand is a string (pathname) then the second operand must evaluate to a number.
            columnParameter = 2;
        }

        Simulator simulator = Simulator.getSimulator (context);
        if (simulator == null) return result;

        Holder H = simulator.outputs.get (path);
        if (H == null)
        {
            H = new Holder ();
            H.simulator = simulator;
            if (path.isEmpty ())
            {
                H.out = simulator.out;
            }
            else
            {
                try
                {
                    H.out = new PrintStream (new File (path).getAbsoluteFile ());
                }
                catch (FileNotFoundException e)
                {
                    H.out = simulator.out;
                }
            }

            if (operands.length > columnParameter + 1)
            {
                H.raw = operands[columnParameter+1].eval (context).toString ().contains ("raw");
            }

            simulator.outputs.put (path, H);
        }

        // Determine column name
        Instance instance;
        if (context instanceof InstanceTemporaries) instance = ((InstanceTemporaries) context).wrapped;
        else                                        instance = context;
        String column = (String) instance.valuesObject[index];
        if (column == null)
        {
            if (operands.length > columnParameter)  // column name is specified
            {
                column = operands[columnParameter].eval (context).toString ();
            }
            else  // auto-generate column name
            {
                String prefix = instance.path ();
                if (prefix.isEmpty ()) column =                variableName;
                else                   column = prefix + "." + variableName;
            }
            instance.valuesObject[index] = column;
        }

        H.trace (column, (float) ((Scalar) result).value);

        return result;
    }

    public Operator simplify (Variable from)
    {
        // Even if our variable is about to be replaced by a constant, we want to present its name in the output column.
        if (variableName == null)
        {
            if (operands[0] instanceof AccessVariable)
            {
                variableName = ((AccessVariable) operands[0]).name;  // the raw name, including prime marks for derivatives
            }
            else
            {
                variableName = from.name;
            }
        }
        return super.simplify (from);
    }

    // This method should be called by analysis, with v set to the variable that holds this equation.
    public void determineVariableName (Variable v)
    {
        boolean needIndex = false;  // Do we need $index to auto-generate names?
        if (operands.length == 1) needIndex = true;
        else if (operands.length == 2)
        {
            // Determine if first parm is a file name.
            class StringVisitor extends Visitor
            {
                boolean foundString;
                public boolean visit (Operator op)
                {
                    if (op instanceof Constant)
                    {
                        Constant c = (Constant) op;
                        if (c.value instanceof Text)
                        {
                            foundString = true;
                            return false;
                        }
                    }
                    return true;
                }
            }
            StringVisitor visitor = new StringVisitor ();
            operands[0].visit (visitor);
            needIndex = visitor.foundString;
        }

        if (needIndex)
        {
            EquationSet container = v.container;
            if (container.connectionBindings == null)  // regular part
            {
                dependOnIndex (v, container);
            }
            else  // connection
            {
                // depend on all endpoints
                for (Entry<String,EquationSet> e : container.connectionBindings.entrySet ())
                {
                    dependOnIndex (v, e.getValue ());
                }
            }
        }
    }

    public void dependOnIndex (Variable v, EquationSet container)
    {
        while (container != null)
        {
            Variable index = container.find (new Variable ("$index"));
            if (index != null  &&  ! container.isSingleton ())
            {
                v.addDependencyOn (index);
            }
            container = container.container;
        }
    }

    public String toString ()
    {
        return "output";
    }
}
