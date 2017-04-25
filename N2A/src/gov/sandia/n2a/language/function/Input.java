/*
Copyright 2016,2017 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.language.function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import gov.sandia.n2a.backend.internal.Simulator;
import gov.sandia.n2a.language.Function;
import gov.sandia.n2a.language.Operator;
import gov.sandia.n2a.language.Type;
import gov.sandia.n2a.language.type.Instance;
import gov.sandia.n2a.language.type.Scalar;
import gov.sandia.n2a.language.type.Text;

public class Input extends Function
{
    public static Factory factory ()
    {
        return new Factory ()
        {
            public String name ()
            {
                return "input";
            }

            public Operator createInstance ()
            {
                return new Input ();
            }
        };
    }

    public static class Holder
    {
        public static final double[] empty = {0};

        public BufferedReader      stream;
        public double              currentLine   = -1;
        public double[]            currentValues = empty;
        public double              nextLine      = -1;
        public double[]            nextValues    = empty;
        public Map<String,Integer> columnMap = new TreeMap<String,Integer> ();
        public boolean             raw;   // indicates that column index should be interpreted as integer rather than real in [0,1]
        public boolean             time;  // indicates that row index should be interpreted as time in seconds
        public int                 timeColumn = 0;
        public boolean             timeColumnSet;

        public void getRow (double requested) throws IOException
        {
            while (true)
            {
                // Read and process next line
                if (nextLine < 0  &&  stream.ready ())
                {
                    String line = stream.readLine ();
                    if (line != null  &&  ! line.isEmpty ())
                    {
                        String[] columns = line.split ("\\s");

                        // Decide whether this is a header row or a value row
                        if (! columns[0].isEmpty ())
                        {
                            char firstCharacter = columns[0].charAt (0);
                            if (firstCharacter < '-'  ||  firstCharacter == '/'  ||  firstCharacter > '9')  // not a number, so must be column header
                            {
                                for (int i = 0; i < columns.length; i++)
                                {
                                    String header = columns[i];
                                    if (! header.isEmpty ()) columnMap.put (header, i);
                                }

                                // Select time column
                                if (time  &&  ! timeColumnSet)
                                {
                                    int timeMatch = 0;
                                    for (Entry<String,Integer> e : columnMap.entrySet ())
                                    {
                                        int potentialMatch = 0;
                                        String header = e.getKey ();
                                        if      (header.equals ("t"   )) potentialMatch = 1;
                                        else if (header.equals ("TIME")) potentialMatch = 2;
                                        else if (header.equals ("$t"  )) potentialMatch = 3;
                                        if (potentialMatch > timeMatch)
                                        {
                                            timeMatch = potentialMatch;
                                            timeColumn = e.getValue ();
                                        }
                                    }
                                    timeColumnSet = true;
                                }

                                continue;  // back to top of outer while loop, skipping any other processing below
                            }
                        }

                        nextValues = new double[columns.length];
                        for (int i = 0; i < columns.length; i++)
                        {
                            String c = columns[i];
                            if (c.isEmpty ()) nextValues[i] = 0;
                            else              nextValues[i] = Double.parseDouble (c);
                        }
                        if (time) nextLine = nextValues[timeColumn];
                        else      nextLine = currentLine + 1;
                    }
                }

                // Determine if we have the requested data
                if (requested <= currentLine) break;
                if (nextLine < 0) break;  // Return the current line, because another is not available. In general, we don't stall the simulator to wait for data.
                if (requested < nextLine) break;
                currentLine   = nextLine;
                currentValues = nextValues;
                nextLine   = -1;
                nextValues = empty;
            }
        }
    }

    public Holder getRow (Instance context)
    {
        Simulator simulator = Simulator.getSimulator (context);
        if (simulator == null) return null;  // If we can't cache a line from the requested stream, then semantics of this function are lost, so give up.

        Holder H = null;
        try
        {
            // get an input holder
            String path = ((Text) operands[0].eval (context)).value;
            H = simulator.inputs.get (path);
            if (H == null)
            {
                H = new Holder ();

                if (path.isEmpty ()) H.stream = new BufferedReader (new InputStreamReader (System.in));  // not ideal; reading stdin should be reserved for headless operation
                else                 H.stream = new BufferedReader (new FileReader (new File (path).getAbsoluteFile ()));

                if (operands.length > 3)
                {
                    String mode = ((Text) operands[3].eval (context)).value;
                    if (mode.contains ("raw" )) H.raw  = true;
                    if (mode.contains ("time")) H.time = true;
                }

                simulator.inputs.put (path, H);
            }
            H.getRow (((Scalar) operands[1].eval (context)).value);
        }
        catch (IOException e)
        {
            return null;
        }

        return H;
    }

    public Type eval (Instance context)
    {
        Holder H = getRow (context);
        if (H == null) return new Scalar (0);

        double column;
        Type columnSpec = operands[2].eval (context);
        if (columnSpec instanceof Text)
        {
            Integer columnMapping = H.columnMap.get (((Text) columnSpec).value);
            if (columnMapping == null) return new Scalar (0);
            column = columnMapping;
        }
        else  // just assume it is a Scalar
        {
            column = ((Scalar) columnSpec).value;
        }

        int columns    = H.currentValues.length;
        int lastColumn = columns - 1;
        if (H.raw)
        {
            int c = (int) Math.round (column);
            if      (c < 0       ) c = 0;
            else if (c >= columns) c = lastColumn;
            return new Scalar (H.currentValues[c]);
        }
        else
        {
            column *= lastColumn;
            int c = (int) Math.floor (column);
            if (c <  0         ) return new Scalar (H.currentValues[0         ]);
            if (c >= lastColumn) return new Scalar (H.currentValues[lastColumn]);
            double b = column - c;
            return new Scalar ((1 - b) * H.currentValues[c] + b * H.currentValues[c+1]);
        }
    }

    public String toString ()
    {
        return "input";
    }
}
