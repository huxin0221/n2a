/*
Copyright 2013-2017 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.language;

@SuppressWarnings("serial")
public class EvaluationException extends RuntimeException
{
    public EvaluationException (String message)
    {
        super (message);
    }
}
