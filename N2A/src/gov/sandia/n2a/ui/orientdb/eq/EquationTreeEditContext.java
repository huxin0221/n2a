/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.ui.orientdb.eq;

import gov.sandia.n2a.language.Annotation;
import gov.sandia.n2a.language.EquationParser;
import gov.sandia.n2a.language.ParsedEquation;
import gov.sandia.n2a.language.gen.ASTNodeBase;
import gov.sandia.n2a.language.gen.ExpressionParser;
import gov.sandia.n2a.language.gen.ParseException;
import gov.sandia.n2a.ui.orientdb.eq.tree.NodeAnnotation;
import gov.sandia.n2a.ui.orientdb.eq.tree.NodeEqReference;
import gov.sandia.n2a.ui.orientdb.eq.tree.NodeEquation;
import gov.sandia.umf.platform.UMF;
import gov.sandia.umf.platform.connect.orientdb.ui.NDoc;
import gov.sandia.umf.platform.ui.UIController;
import gov.sandia.umf.platform.ui.images.ImageUtil;
import gov.sandia.umf.platform.ui.search.SearchType;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

import replete.event.ChangeNotifier;
import replete.gui.controls.simpletree.NodeBase;
import replete.gui.controls.simpletree.SimpleTree;
import replete.gui.controls.simpletree.TModel;
import replete.gui.controls.simpletree.TNode;
import replete.gui.windows.common.CommonFrame;

public class EquationTreeEditContext {


    ////////////
    // FIELDS //
    ////////////

    // Core

    public UIController uiController;
    public SimpleTree tree;
    public Class<?> addToClass;

    // Derived

    public TModel model;


    //////////////
    // NOTIFIER //
    //////////////

    protected ChangeNotifier eqChangeNotifier = new ChangeNotifier(this);
    public void addEqChangeListener(ChangeListener listener) {
        eqChangeNotifier.addListener(listener);
    }
    protected void fireEqChangeNotifier() {
        eqChangeNotifier.fireStateChanged();
    }


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public EquationTreeEditContext(UIController uic, SimpleTree t, Class<?> cls) {
        uiController = uic;
        tree = t;
        addToClass = cls;

        model = tree.getTModel();
    }


    /////////////
    // ACTIONS //
    /////////////

    // Add

    public void addEquation(String initVal) {
        TNode chosenNode = getContextRoot(null);
        if(chosenNode != null) {
            EquationInputBox input = getEquationInputBox(false, false, initVal == null ? "" : initVal);
            input.setVisible(true);
            if(input.getResult() == EquationInputBox.Result.OK) {
                try{
                    ParsedEquation peq = EquationParser.parse(input.getValue());
                    // TODO: Check to see if exists in tree first...
                    NDoc eq = new NDoc("gov.sandia.umf.n2a$Equation");  // TODO: Is this a magic string?
                    eq.set("value", input.getValue());
                    NodeEquation uEqn = new NodeEquation(eq);
                    TNode nEq = model.append(chosenNode, uEqn);
                    for(Annotation an : peq.getAnnotations().values()) {
                        NodeAnnotation uAnnot = new NodeAnnotation(an);
                        model.append(nEq, uAnnot);
                    }
                    tree.select(nEq);
                    fireEqChangeNotifier();
                } catch(Exception ex) {
                    UMF.handleUnexpectedError(null, ex, "An error has occurred adding the equation to the tree.");
                }
            }
        }
    }

    public void addEquations(List<ParsedEquation> peqs) {
        TNode chosenNode = getContextRoot(null);
        if(chosenNode != null) {
            for(ParsedEquation peq : peqs) {
                // TODO: Check to see if exists in tree first...
                NDoc eq = new NDoc("gov.sandia.umf.n2a$Equation");  // TODO: Is this a magic string?
                eq.set("value", peq.getSource());
                NodeEquation uEqn = new NodeEquation(eq);
                TNode nEq = model.append(chosenNode, uEqn);
                for(Annotation an : peq.getAnnotations().values()) {
                    NodeAnnotation uAnnot = new NodeAnnotation(an);
                    model.append(nEq, uAnnot);
                }
            }
            tree.select(chosenNode);
            fireEqChangeNotifier();
        }
    }

    public void addAnnotationToSelected() {
        TNode contextRoot = getContextRoot(NodeEquation.class);
        if(contextRoot != null) {
            EquationInputBox input = getEquationInputBox(false, true, "");
            input.setVisible(true);
            if(input.getResult() == EquationInputBox.Result.OK) {
                try {
                    String annoText = input.getValue();
                    ASTNodeBase annoPe = ExpressionParser.parse(annoText);
                    Annotation an = new Annotation(annoPe);
                    NodeAnnotation uNew = new NodeAnnotation(an);
                    TNode nNew = model.append(contextRoot, uNew);
                    tree.select(nNew);
                    fireEqChangeNotifier();
                } catch(Exception ex) {
                    UMF.handleUnexpectedError(null, ex, "An error has occurred adding the annotation to the tree.");
                }
            }
        }
    }

    public void addReferenceToSelected() {
        TreePath path = tree.getSelectionPath();
        if(path != null) {
            String searchTitle = "Add Reference For Equation";
            CommonFrame parentWin = (CommonFrame) SwingUtilities.getRoot(tree);
            List<NDoc> chosen = uiController.searchRecordOrient(parentWin, SearchType.REFERENCE, searchTitle,
                null, ImageUtil.getImage("complete.gif"), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            if(chosen != null) {
                for(NDoc record : chosen) {
                    try {
                        // This could be cleaned up with supporting methods.
                        TNode nSel = (TNode) path.getLastPathComponent();
                        if(nSel.getUserObject() instanceof NodeAnnotation || nSel.getUserObject() instanceof NodeEqReference) {
                            nSel = (TNode) nSel.getParent();
                        }
                        NDoc eq = ((NodeEquation) nSel.getUserObject()).getEq();
                        NDoc eqRef = new NDoc();
                        eqRef.set("ref", record);
                        List<NDoc> refs = eq.getValid("refs", new ArrayList<NDoc>(), List.class);
                        refs.add(eqRef);
                        NodeEqReference uER = new NodeEqReference(eqRef);
                        TNode nNew = model.append(nSel, uER);
                        tree.select(nNew);
                        fireEqChangeNotifier();
                    } catch(Exception ex) {
                        UMF.handleUnexpectedError(null, ex, "An error has occurred adding the reference to the tree.");
                    }
                }
            }
        }
    }

    // Edit

    public void editSelectedNode() {
        TreePath selPath = tree.getSelectionPath();
        if(selPath == null) {
            return;
        }

        // These two blocks probably could be combined in some fashion.
        TNode nSel = (TNode) selPath.getLastPathComponent();
        NodeBase uSel = (NodeBase) nSel.getUserObject();

        if(uSel instanceof NodeEqReference) {
            NDoc eqRef = ((NodeEqReference) uSel).getEqReference();
            uiController.openRecord((NDoc) eqRef.getValid("ref", null, NDoc.class));
        } else if(uSel instanceof NodeEquation) {
            editEquationNode(nSel);
        } else if(uSel instanceof NodeAnnotation) {
            editAnnotationNode(nSel);
        }
    }

    public void editAnnotationNode(TNode nSel) {
        try {
            NodeAnnotation uSel = (NodeAnnotation) nSel.getUserObject();
            String curVal = uSel.getAnnotation().getTree().getSource();
            EquationInputBox input = getEquationInputBox(true, true, curVal);
            input.setVisible(true);
            if(input.getResult() == EquationInputBox.Result.OK) {
                String annoText = input.getValue();
                ASTNodeBase annoPe = ExpressionParser.parse(annoText);
                uSel.setAnnotation(new Annotation(annoPe));
                tree.select(nSel);
                fireEqChangeNotifier();
            }
        } catch(ParseException ex) {
            UMF.handleUnexpectedError(null, ex, "An error has occurred editing the annotation.");
        }
    }

    public void editEquationNode(TNode nSel) {
        try {
            NodeEquation uSel = (NodeEquation) nSel.getUserObject();
            NDoc eq = uSel.getEq();
            String curVal = EquationParser.parse((String) eq.get("value")).getTree().getSource();
            EquationInputBox input = getEquationInputBox(true, false, curVal);
            input.setVisible(true);
            if(input.getResult() == EquationInputBox.Result.OK) {
                ParsedEquation peq = EquationParser.parse(input.getValue());
                uSel.setEqValue(input.getValue());
                if(peq.getAnnotations().size() > 0) {
                    model.remove(nSel, NodeAnnotation.class);
                    for(Annotation an : peq.getAnnotations().values()) {
                        NodeAnnotation uAnnot = new NodeAnnotation(an);
                        model.append(nSel, uAnnot);
                    }
                }
                tree.select(nSel);
                fireEqChangeNotifier();
            }
        } catch(ParseException ex) {
            UMF.handleUnexpectedError(null, ex, "An error has occurred editing the equation.");
        }
    }

    // Remove

    // TODO: Can't remove annotation and reference nodes that aren't in the right spot.
    public void removeSelectedNode() {
        if(tree.getSelectionPath() == null) {
            return;
        }

        // Group all the various nodes that are selected
        // by their user objects' classes so that the various
        // types of nodes can be deleted in the appropriate
        // order.
        Map<Class<?>, List<TNode>> toDel = new HashMap<Class<?>, List<TNode>>();
        TreePath[] paths = tree.getSelectionPaths();
        for(TreePath path : paths) {
            TNode nAny = (TNode) path.getLastPathComponent();
            Class<?> clazz = nAny.getUserObject().getClass();
            List<TNode> delNodes = toDel.get(clazz);
            if(delNodes == null) {
                delNodes = new ArrayList<TNode>();
                toDel.put(clazz, delNodes);
            }
            delNodes.add(nAny);
        }

        // First the annotations and equation references.
        if(toDel.get(NodeAnnotation.class) != null) {
            for(TNode node : toDel.get(NodeAnnotation.class)) {
                removeNode(node);
            }
        }
        if(toDel.get(NodeEqReference.class) != null) {
            for(TNode node : toDel.get(NodeEqReference.class)) {
                removeNode(node);
            }
        }

        // Then the equations.
        if(toDel.get(NodeEquation.class) != null) {
            for(TNode node : toDel.get(NodeEquation.class)) {
                removeNode(node);
            }
        }

        tree.updateUI();
        fireEqChangeNotifier();
    }

    public void removeNode(TNode node) {
        if(node.getUserObject() instanceof NodeEquation) {
            tree.remove(node);
        } else if(node.getUserObject() instanceof NodeAnnotation || node.getUserObject() instanceof NodeEqReference) {
            tree.remove(node);
            /*
            TNode eq = (TNode) node.getParent();
            TNode sib = (TNode) node.getNextSibling();
            if(sib == null) {
                sib = (TNode) node.getPreviousSibling();
            }
            model.removeNodeFromParent(node);
            if(sib != null) {
                tree.setSelectionPath(myPath(sib));
            } else {
                tree.setSelectionPath(myPath(eq));
            }
            */
        }
    }

    // Move

    // TODO: Group move down.
    public void moveSelectedDown() {
        TNode contextRoot = getContextRoot(null);
        if(contextRoot != null) {
            TreePath selPath = tree.getSelectionPath();
            TNode nSel = (TNode) selPath.getLastPathComponent();
            int curIndex = contextRoot.getIndex(nSel);
            if(curIndex < contextRoot.getChildCount() - 1) {
                contextRoot.insert(nSel, curIndex + 1);
                tree.updateUI();       // Model changed
                fireEqChangeNotifier();
            }
        }
    }

    // TODO: Group move up.
    public void moveSelectedUp() {
        TNode contextRoot = getContextRoot(null);
        if(contextRoot != null) {
            TreePath selPath = tree.getSelectionPath();
            TNode nSel = (TNode) selPath.getLastPathComponent();
            int curIndex = contextRoot.getIndex(nSel);
            if(curIndex > 0) {
                contextRoot.insert(nSel, curIndex - 1);
                tree.updateUI();       // Model changed
                fireEqChangeNotifier();
            }
        }
    }


    //////////
    // MISC //
    //////////

    private TNode getContextRoot(Class<?> stopAtClass) {
        TNode chosenNode = null;
        TreePath path = tree.getSelectionPath();
        if(path != null) {
            return getContextRoot(stopAtClass, (TNode) path.getLastPathComponent());
        } else if(addToClass == null) {
            return getContextRoot(stopAtClass, null);
        }
        return chosenNode;
    }
    private TNode getContextRoot(Class<?> stopAtClass, TNode nSel) {
        TNode chosenNode = null;
        TNode n = nSel;
        while(n != null) {
            Class<?> uClass = n.getUserObject().getClass();
            if(stopAtClass != null) {
                if(stopAtClass.isAssignableFrom(uClass)) {
                    chosenNode = n;
                    break;
                }
            } else if(addToClass != null && addToClass.isAssignableFrom(uClass)) {
                chosenNode = n;
                break;
            }
            n = (TNode) n.getParent();
        }
        if(addToClass == null && stopAtClass == null) {
            chosenNode = (TNode) model.getRoot();
        }
        return chosenNode;
    }

    private EquationInputBox getEquationInputBox(boolean isEdit, boolean annot, String initVal) {
        Component c = SwingUtilities.getRoot(tree);
        EquationInputBox input;
        if(c instanceof JFrame) {
            input = new EquationInputBox((JFrame) c, isEdit, annot, initVal);
        } else {
            input = new EquationInputBox((JDialog) c, isEdit, annot, initVal);
        }
        return input;
    }

    public List<NDoc> getEquations(TNode contextRoot) {
        List<NDoc> eqs = new ArrayList<NDoc>();
        for(int i = 0; i < contextRoot.getChildCount(); i++) {
            TNode nEq = (TNode) contextRoot.getChildAt(i);
            NodeEquation uEq = (NodeEquation) nEq.getUserObject();
            String line = uEq.getParsed().getTree().getSource();
            NDoc eq = uEq.getEq();
            eq.set("order", i);                  // Order is updated here upon access.
            List<NDoc> eqRefs = new ArrayList<NDoc>();
            for(int j = 0; j < nEq.getChildCount(); j++) {
                TNode nChild = (TNode) nEq.getChildAt(j);
                if(nChild.getUserObject() instanceof NodeAnnotation) {
                    NodeAnnotation uAn = (NodeAnnotation) nChild.getUserObject();
                    line += "  @" + uAn.getAnnotation().getTree().getSource();
                } else if(nChild.getUserObject() instanceof NodeEqReference) {
                    NodeEqReference uEqRef = (NodeEqReference) nChild.getUserObject();
                    eqRefs.add(uEqRef.getEqReference());
                }
            }
            eq.set("refs", eqRefs);
            uEq.setEqValue(line);
            eqs.add(eq);
        }
        return eqs;
    }

    public void setEquations(TNode targetNode, List<NDoc> eqs) {
        targetNode.removeAllChildren();

        // Construct an ORDER-BIN#->LIST map to handle any
        // combination of values for the order fields as
        // they exist in the database.
        Map<Integer, List<NDoc>> orderedEqs = new TreeMap<Integer, List<NDoc>>();
        for(NDoc eq : eqs) {
            Integer order = eq.get("order", null);
            if(order == null) {
                order = Integer.MAX_VALUE;
            }
            List<NDoc> binEqs = orderedEqs.get(order);
            if(binEqs == null) {
                binEqs = new ArrayList<NDoc>();
                orderedEqs.put(order, binEqs);
            }
            binEqs.add(eq);
        }

        for(Integer order : orderedEqs.keySet()) {
            for(NDoc eq : orderedEqs.get(order)) {
                NodeEquation uEqn = new NodeEquation(eq);
                TNode nEqn = new TNode(uEqn);
                model.insertNodeInto(nEqn, targetNode, targetNode.getChildCount());
                for(Annotation an : uEqn.getParsed().getAnnotations().values()) {
                    NodeAnnotation uAnnot = new NodeAnnotation(an);
                    TNode nAnnot = new TNode(uAnnot);
                    model.insertNodeInto(nAnnot, nEqn, nEqn.getChildCount());
                }
                for(NDoc eqRef : (List<NDoc>) eq.getValid("refs", new ArrayList<NDoc>(), List.class)) {
                    NodeEqReference uER = new NodeEqReference(eqRef);
                    TNode nER = new TNode(uER);
                    model.insertNodeInto(nER, nEqn, nEqn.getChildCount());
                }
                TreePath newPath = new TreePath(new Object[]{targetNode, nEqn});
                tree.expandPath(newPath);
            }
        }
    }
    public boolean isInContext(TNode nAny) {
        return getContextRoot(addToClass, nAny) != null;
    }
}
