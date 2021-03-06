/*
Copyright 2017 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.ui.eq.undo;

import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import gov.sandia.n2a.db.MNode;
import gov.sandia.n2a.db.MVolatile;
import gov.sandia.n2a.eqset.MPart;
import gov.sandia.n2a.ui.Undoable;
import gov.sandia.n2a.ui.eq.FilteredTreeModel;
import gov.sandia.n2a.ui.eq.PanelModel;
import gov.sandia.n2a.ui.eq.tree.NodeAnnotations;
import gov.sandia.n2a.ui.eq.tree.NodeBase;
import gov.sandia.n2a.ui.eq.tree.NodeContainer;

public class AddAnnotations extends Undoable
{
    protected List<String> path;  ///< to parent of $metadata node
    protected int          index; ///< Position within parent node
    protected MVolatile    saved; ///< subtree under $metadata

    public AddAnnotations (NodeBase parent, int index, MNode data)
    {
        path = parent.getKeyPath ();
        this.index = index;

        saved = new MVolatile ("$metadata", "");
        saved.merge (data);
    }

    public void undo ()
    {
        super.undo ();
        destroy (path, saved.key ());
    }

    public static void destroy (List<String> path, String blockName)
    {
        NodeBase parent = NodeBase.locateNode (path);
        if (parent == null) throw new CannotUndoException ();

        PanelModel mep = PanelModel.instance;
        JTree tree = mep.panelEquations.tree;
        FilteredTreeModel model = (FilteredTreeModel) tree.getModel ();

        NodeContainer node = (NodeContainer) parent.child (blockName);
        TreeNode[] nodePath = node.getPath ();
        int index = parent.getIndexFiltered (node);

        MPart mparent = parent.source;
        mparent.clear (blockName);
        if (mparent.child (blockName) == null)
        {
            model.removeNodeFromParent (node);
        }
        else  // Just exposed an overridden node
        {
            node.build ();  // Necessary to remove all overridden nodes
            node.filter (model.filterLevel);
        }
        mep.panelEquations.updateVisibility (nodePath, index);
    }

    public void redo ()
    {
        super.redo ();
        NodeFactory factory = new NodeFactory ()
        {
            public NodeBase create (MPart part)
            {
                return new NodeAnnotations (part);
            }
        };
        create (path, index, saved, factory);
    }

    public static void create (List<String> path, int index, MNode saved, NodeFactory factory)
    {
        NodeBase parent = NodeBase.locateNode (path);
        if (parent == null) throw new CannotRedoException ();
        String blockName = saved.key ();
        NodeBase n = parent.child (blockName);
        if (n != null  &&  ! (n instanceof NodeContainer)) throw new CannotRedoException ();
        NodeContainer node = (NodeContainer) n;

        MPart block = (MPart) parent.source.childOrCreate (blockName);
        block.merge (saved);

        PanelModel mep = PanelModel.instance;
        JTree tree = mep.panelEquations.tree;
        FilteredTreeModel model = (FilteredTreeModel) tree.getModel ();

        if (node == null)
        {
            node = (NodeContainer) factory.create (block);
            model.insertNodeIntoUnfiltered (node, parent, index);
        }
        node.build ();  // Replaces all nodes, so they are set to require tab initialization.
        node.filter (model.filterLevel);
        mep.panelEquations.updateVisibility (node.getPath ());
    }
}
