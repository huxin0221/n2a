/*
Copyright 2016 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.eqset;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

import gov.sandia.umf.platform.db.AppData;
import gov.sandia.umf.platform.db.MNode;
import gov.sandia.umf.platform.db.MPersistent;
import gov.sandia.umf.platform.db.MVolatile;

/**
    Collates models following all the N2A language rules, and provides an interface
    for active editing. Also suitable for construction of EquationSets for execution.
    Implements the full MNode interface on the collated tree, as if it were nothing
    more than a regular MNode tree. Changes made through this interface cause changes
    in the original top-level document that would otherwise produce the resulting tree.
    Additional functions (outside the main MNode interface) support efficient
    construction.

    A key assumption of this implementation is that only MPersistent nodes are fed into it.
**/
public class MPart extends MNode  // Could derive this from MVolatile, but the extra work of implementing from scratch is worth saving an unused member variable.
{
    protected boolean     fromTopDocument;  // Indicates that this node is overridden by the top-level document.
    protected MPersistent source;
    protected MPersistent original;  // The original source of this node, before it was overwritten by another document. Refers to same object as source if this node has not been overwritten, or if it is originally from the top-level doc (ie: is the first level of children under the root).
    // Note: It is always possible to retrieve the top-level documents by following parents up. If this proves too slow, then add fields here to reference the top docs directly.

    protected MPart container;
    protected NavigableMap<String,MNode> children;

    public MPart (MPart container, MPersistent source, boolean fromTopDocument)
    {
        this.fromTopDocument = fromTopDocument;
        this.container       = container;
        this.source          = source;
        original             = source;
    }

    public MPart (MPart container, MPersistent source, MPersistent original, boolean fromTopDocument)
    {
        this.fromTopDocument = fromTopDocument;
        this.container       = container;
        this.source          = source;
        this.original        = original;
    }

    /**
        Constructs a fully-collated tree, processing all $include and $inherit statements.
    **/
    public static MPart collate (MPersistent source) throws Exception
    {
        return collate (null, source);
    }

    /**
        Constructs a fully-collated tree, processing all $include and $inherit statements.
        Assumes we are always building the top-level document, regardless of whether we are in
        a container of not.
        @param container If non-null, we perform a recursion check to ensure that a part does not
        inherit or include itself.
    **/
    public static MPart collate (MPart container, MPersistent source) throws Exception
    {
        // Recursion check
        while (container != null)
        {
            if (container.source == source)  // MDir guarantees that its MDocs have object identity, so direct comparison of references is sufficient.
            {
                throw new Exception ("Self-referential loop in part: " + source);
            }
            container = container.container;  // I guess that would be grandparent.
        }

        MPart result = new MPart (container, source, true);
        if (source.length () == 0) return result;
        result.children = new TreeMap<String,MNode> (comparator);

        // Inherits
        MNode inherit = source.child ("$inherit");
        if (inherit != null)
        {
            String value = inherit.get ();
            String[] partNames = value.split (",");
            for (String partName : partNames)
            {
                partName = partName.trim ().replace ("\"", "");
                MPersistent partSource = (MPersistent) AppData.getInstance ().models.child (partName);
                if (partSource != null) result.mergeChildren (collate (container, partSource));
            }
        }
        result.resetOverride ();  // all inherited equations are by definition not from the top document
        result.fromTopDocument = true;  // the above resetOverride() is a somewhat blunt method, so restore our true status

        // Regular equations, as well as $include lines (sub-parts)
        for (MNode e : source)
        {
            MPart equation = new MPart (result, (MPersistent) e, true);
            String index = e.key ();
            if (index.equals ("$inherit"))
            {
                result.children.put ("$inherit", equation);  // Replaces any $inherit we may have gotten from parents, which refer to grandparents and farther ancestors.
                continue;
            }
            equation.buildTree ();

            String value = e.get ();
            if (value.contains ("$include"))
            {
                MPersistent partSource = null;
                String[] pieces = value.split ("\"");  // TODO: more resilient parsing
                if (pieces.length > 1) partSource = (MPersistent) AppData.getInstance ().models.child (pieces[1]);
                if (partSource != null)
                {
                    MPart part = collate (result, partSource);
                    part.resetOverride ();  // an included equation set is by definition not from the top document
                    part.mergeChildren (equation);  // Anything that goes under a $include line is intended to be an override of the included part.
                    equation.children = part.children;  // Transfer the children back over to the equation to form the final tree.
                    for (MNode c : equation) ((MPart) c).container = equation;
                }
            }

            // Merge a single child. Could have a separate function, but this is the only place we do this.
            MPart existing = (MPart) result.child (index);
            if (existing == null) result.children.put (index, equation);
            else existing.mergePart (equation);
        }

        return result;
    }

    /**
        Given a tree that is fully built, rebuild the sub-tree under a $include statement.
        Also handle the case where the $include is removed. In this case, there might still be
        an equation tree in the top-document that is meant to function as a sub-part.
        This function should only be called on nodes that currently have fromTopDocument==true.
    **/
    public synchronized void update ()
    {
        // This is basically a copy of the equation-processing loop in collate(), but with some stronger assumptions.
        if (children != null) clearRecursive (false);  // reset nodes derived from top document back to their non-overridden state
        // Build a new equation node, separately from myself because I may be carrying inherited equations
        MPart equation = new MPart (container, source, true);
        equation.buildTree ();
        String value = source.get ();
        if (value.contains ("$include"))
        {
            MPersistent partSource = null;
            String[] pieces = value.split ("\"");
            if (pieces.length > 1) partSource = (MPersistent) AppData.getInstance ().models.child (pieces[1]);
            if (partSource != null)
            {
                MPart part;
                try
                {
                    part = collate (container, partSource);
                    part.resetOverride ();
                    part.mergeChildren (equation);
                    equation.children = part.children;
                    for (MNode c : equation) ((MPart) c).container = equation;
                }
                catch (Exception e)
                {
                    System.err.println ("Failed to construct included tree: " + e);
                }
            }
        }
        mergeChildren (equation);
    }

    /**
        Incorporate a newly-created node of the document into the existing collated tree.
        Assumes this node represents the parent of the given source, and that none of our
        children represent the source itself.
    **/
    public synchronized MPart update (MPersistent source)
    {
        MPart c = new MPart (this, source, true);
        children.put (source.key (), c);
        c.update ();
        return c;
    }

    public synchronized void resetOverride ()
    {
        fromTopDocument = false;
        for (MNode i : this) ((MPart) i).resetOverride ();
    }

    /**
        Expands our source into children, without trying to interpret any of the equations (such as $inherit or $include).
        Assumes that we currently have no children.
    **/
    public synchronized void buildTree ()
    {
        if (source.length () == 0) return;
        if (children == null) children = new TreeMap<String,MNode> (comparator);
        for (MNode n : source)
        {
            MPart p = new MPart (this, (MPersistent) n, fromTopDocument);
            children.put (n.key (), p);
            p.buildTree ();
        }
    }

    /**
        Overrides this part with the given part, then apply recursively to children.
    **/
    public synchronized void mergePart (MPart that)
    {
        if (source != that.source)
        {
            fromTopDocument = that.fromTopDocument;
            original = source;
            source = that.source;
        }
        mergeChildren (that);
    }

    /**
        Overrides our children with the children of the given part.
    **/
    public synchronized void mergeChildren (MPart that)
    {
        if (that.length () == 0) return;
        if (children == null) children = new TreeMap<String,MNode> (comparator);
        for (MNode n : that)
        {
            MPart p = (MPart) n;
            String index = p.key ();
            MPart existing = (MPart) children.get (index);
            if (existing == null)
            {
                MPart newPart = new MPart (this, p.source, p.original, p.fromTopDocument);
                children.put (index, newPart);
                newPart.children = p.children;  // The rest of the tree can simply be copied over, assuming "that" will not be re-used elsewhere.
            }
            else existing.mergePart (p);
        }
    }

    /**
        Indicates if this node has the form of a sub-part.
        Assumes the caller has already eliminated the cases of $metadata and $reference, so does not perform extra checks for these keys.
    **/
    public boolean isPart ()
    {
        String value = source.get ();
        if (value.contains ("$include")) return true;
        if (! value.isEmpty ()) return false;
        for (MNode c : this) if (c.key ().startsWith ("@")) return false;  // has the form of a multi-line equation
        return true;
    }

    public MPart getParent ()
    {
        return container;
    }

    public MPart getRoot ()
    {
        MPart result = this;
        while (result.container != null) result = result.container;
        return result;
    }

    public MPersistent getSource ()
    {
        return source;
    }

    public synchronized MNode child (String index)
    {
        if (children == null) return null;
        return children.get (index);
    }

    public String key ()
    {
        return source.key ();  // could also use original.key(), as they should always match
    }

    /**
        Removes all children of this node from the top-level document, and restores them to their non-overridden state.
        Some of the nodes may disappear, if they existed only due to override.
        In general, acts as if the clear is applied in the top-level document, followed by a full collation of the tree.
    **/
    public synchronized void clear ()
    {
        if (children == null) return;
        if (! fromTopDocument) return; // Nothing to do.
        clearRecursive (true);
        clearPath ();
    }

    /**
        Assuming that source in the current node belongs to the top-level document, reset all overridden children back to their original state.
    **/
    public synchronized void clearRecursive (boolean removeFromTopDocument)
    {
        Iterator<MNode> i = source.iterator ();  // Implicitly, everything we iterate over will be from the top document.
        while (i.hasNext ())
        {
            String key = i.next ().key ();
            MPart c = (MPart) children.get (key);  // This should exist, unless a bug somewhere rendered the tree inconsistent.
            c.clearRecursive (removeFromTopDocument);
            if (c.source == c.original)  // The child existed solely through override, so remove it completely.
            {
                children.remove (key);
            }
            else  // Otherwise, restore the original value.
            {
                c.fromTopDocument = false;
                c.source = c.original;
            }
            if (removeFromTopDocument) i.remove ();
        }
    }

    /**
        Removes the named child of this node from the top-level document, and restores it to its non-overridden state.
        The child may disappear, if it existed only due to override.
        In general, acts as if the clear is applied in the top-level document, followed by a full collation of the tree.
    **/
    public synchronized void clear (String index)
    {
        clear (index, true);
    }

    public synchronized void clear (String index, boolean removeFromTopDocument)
    {
        if (children == null) return;
        if (! fromTopDocument) return;  // This node is not overridden, so none of the children will be.
        if (source.child (index) == null) return;  // The child is not overridden, so nothing to do.

        // Actually clear the child
        MPart c = (MPart) children.get (index);
        c.clearRecursive (removeFromTopDocument);
        if (c.source == c.original)
        {
            children.remove (index);
        }
        else
        {
            c.fromTopDocument = false;
            c.source = c.original;
        }
        if (removeFromTopDocument)
        {
            source.clear (index);
            clearPath ();
        }
    }

    public synchronized int length ()
    {
        if (children == null) return 0;
        return children.size ();
    }

    public synchronized String getOrDefault (String defaultValue)
    {
        return source.getOrDefault (defaultValue);
    }

    /**
        Extends the trail of overrides from the root to this node.
        Used to prepare this node for modification, where all edits must reside in top-level document.
    **/
    public synchronized void override ()
    {
        if (fromTopDocument) return;
        container.override ();
        original = source;
        source = (MPersistent) container.source.set ("", key ());
        fromTopDocument = true;
    }

    public boolean isFromTopDocument ()
    {
        return fromTopDocument;
    }

    /**
        Checks if any child is overridden. If so, then then current node must remain overridden as well.
    **/
    public synchronized boolean overrideNecessary ()
    {
        for (MNode c : this) if (((MPart) c).fromTopDocument) return true;
        return false;
    }

    /**
        If an override is no longer needed on this node, reset it to its original state, and make
        a recursive call to our parent. This is effectively the anti-operation to override().

        @TODO There is an issue with clearing when the path leads through a $include node.
        The override path only has the name of the part, not the full $include statement. Thus the
        value never reverts to the original, so we never detect it. Switching to a "lay-under"
        scheme rather than the current "lay-over" scheme (as implemented in collate()) should resolve this.
    **/
    public synchronized void clearPath ()
    {
        if (fromTopDocument  &&  source != original  &&  source.get ().equals (original.get ())  &&  ! overrideNecessary ())
        {
            source.getParent ().clear (source.key ());  // delete ourselves from the top-level document
            source = original;
            fromTopDocument = false;
            container.clearPath ();
        }
    }

    public synchronized void set (String value)
    {
        if (source.get ().equals (value)) return;  // No change, so nothing to do.
        override ();
        source.set (value);
        clearPath ();
    }

    public synchronized MNode set (String value, String index)
    {
        MPart result = null;
        if (children != null) result = (MPart) children.get (index);
        if (result != null)
        {
            result.set (value);
            return result;
        }

        // We don't have the child, so by construction it is not in any source document.
        override ();
        MPersistent s = (MPersistent) source.set (value, index);  // override() ensures that source is a member of the top-level document tree
        result = new MPart (this, s, true);
        if (children == null) children = new TreeMap<String,MNode> (comparator);
        children.put (index, result);
        return result;
    }

    /**
        For debugging the assembled tree.
    **/
    public synchronized void dump (String space)
    {
        String index = key ();
        String value = get ();
        if (value.isEmpty ())
        {
            System.out.println (String.format ("%s%s", space, index) + "  (" + dumpHash (this) + ", " + dumpHash (container) + ")  " + fromTopDocument);
        }
        else
        {
            String newLine = String.format ("%n");
            if (value.contains (newLine))  // go into extended text write mode
            {
                value = value.replace (newLine, newLine + space + "  ");
                value = "|" + newLine + space + "  " + value;
            }
            System.out.println (String.format ("%s%s=%s", space, index, value) + "  (" + dumpHash (this) + ", " + dumpHash (container) + ")  " + fromTopDocument);
        }

        String space2 = space + " ";
        for (MNode c : this) ((MPart) c).dump (space2);
    }

    public static String dumpHash (MPart part)
    {
        if (part == null) return null;
        return String.valueOf (part.hashCode ());
    }

    public synchronized Iterator<MNode> iterator ()
    {
        if (children == null) return new MNode.IteratorEmpty ();
        return new MVolatile.IteratorWrapper (children.entrySet ().iterator ());
    }
}