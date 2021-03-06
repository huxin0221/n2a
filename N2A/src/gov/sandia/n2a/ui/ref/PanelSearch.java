/*
Copyright 2017-2018 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.ui.ref;

import gov.sandia.n2a.db.AppData;
import gov.sandia.n2a.db.MDir;
import gov.sandia.n2a.db.MDoc;
import gov.sandia.n2a.db.MNode;
import gov.sandia.n2a.db.MVolatile;
import gov.sandia.n2a.ui.Lay;
import gov.sandia.n2a.ui.SafeTextTransferHandler;
import gov.sandia.n2a.ui.ref.undo.AddEntry;
import gov.sandia.n2a.ui.ref.undo.DeleteEntry;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

@SuppressWarnings("serial")
public class PanelSearch extends JPanel
{
    public JTextField              textQuery;
    public JList<MNode>            list;
    public DefaultListModel<MNode> model;
    public int                     lastSelection = -1;
    public int                     insertAt;
    public MNodeRenderer           renderer = new MNodeRenderer ();

    public PanelSearch ()
    {
        list = new JList<MNode> (model = new DefaultListModel<MNode> ());
        list.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
        list.setDragEnabled (true);
        list.setCellRenderer (renderer);

        InputMap inputMap = list.getInputMap ();
        inputMap.put (KeyStroke.getKeyStroke ("INSERT"),     "add");
        inputMap.put (KeyStroke.getKeyStroke ("DELETE"),     "delete");
        inputMap.put (KeyStroke.getKeyStroke ("BACK_SPACE"), "delete");
        inputMap.put (KeyStroke.getKeyStroke ("ENTER"),      "select");

        ActionMap actionMap = list.getActionMap ();
        actionMap.put ("add", new AbstractAction ()
        {
            public void actionPerformed (ActionEvent e)
            {
                PanelReference.instance.undoManager.add (new AddEntry ());
            }
        });
        actionMap.put ("delete", new AbstractAction ()
        {
            public void actionPerformed (ActionEvent e)
            {
                MNode deleteMe = list.getSelectedValue ();
                if (deleteMe == null  ||  ! AppData.references.isWriteable (deleteMe)) return;
                lastSelection = list.getSelectedIndex ();
                PanelReference.instance.undoManager.add (new DeleteEntry ((MDoc) deleteMe));
            }
        });
        actionMap.put ("select", new AbstractAction ()
        {
            public void actionPerformed (ActionEvent e)
            {
                selectCurrent ();
            }
        });

        list.addMouseListener (new MouseAdapter ()
        {
            public void mouseClicked (MouseEvent e)
            {
                if (e.getClickCount () > 1) selectCurrent ();
            }
        });

        list.addFocusListener (new FocusListener ()
        {
            public void focusGained (FocusEvent e)
            {
                showSelection ();
            }

            public void focusLost (FocusEvent e)
            {
                hideSelection ();
            }
        });

        list.setTransferHandler (new TransferHandler ()
        {
            public boolean canImport (TransferSupport xfer)
            {
                return xfer.isDataFlavorSupported (DataFlavor.stringFlavor);
            }

            public boolean importData (TransferSupport xfer)
            {
                if (! list.isFocusOwner ()) hideSelection ();

                ParserBibtex parser = new ParserBibtex ();
                MNode data = new MVolatile ();
                try
                {
                    StringReader reader = new StringReader ((String) xfer.getTransferable ().getTransferData (DataFlavor.stringFlavor));
                    parser.parse (reader, data);
                }
                catch (IOException | UnsupportedFlavorException e)
                {
                    return false;
                }

                for (MNode n : data)  // data can contain several entries
                {
                    String key = MDir.validFilenameFrom (n.key ());
                    PanelReference.instance.undoManager.add (new AddEntry (key, n));
                }
                return true;
            }

            public int getSourceActions (JComponent comp)
            {
                return COPY;
            }

            protected Transferable createTransferable (JComponent comp)
            {
                MNode ref = list.getSelectedValue ();
                if (ref == null) return null;

                StringWriter writer = new StringWriter ();
                try
                {
                    String nl = String.format ("%n");
                    writer.write ("@" + ref.get ("form") + "{" + ref.key () + "," + nl);
                    for (MNode c : ref) writer.write ("  " + c.key () + "={" + c.get () + "}," + nl);
                    writer.write ("}" + nl);
                    writer.close ();
                    return new StringSelection (writer.toString ());
                }
                catch (IOException e)
                {
                }

                return null;
            }

            protected void exportDone (JComponent source, Transferable data, int action)
            {
                if (! list.isFocusOwner ()) hideSelection ();
            }
        });


        textQuery = new JTextField ();

        textQuery.addKeyListener (new KeyAdapter ()
        {
            public void keyReleased (KeyEvent e)
            {
                if (e.getKeyCode () == KeyEvent.VK_ESCAPE) textQuery.setText ("");
            }
        });

        textQuery.getDocument ().addDocumentListener (new DocumentListener ()
        {
            public void insertUpdate (DocumentEvent e)
            {
                search ();
            }

            public void removeUpdate (DocumentEvent e)
            {
                search ();
            }

            public void changedUpdate (DocumentEvent e)
            {
                search ();
            }
        });

        textQuery.setTransferHandler (new SafeTextTransferHandler ()
        {
            public boolean importData (TransferSupport support)
            {
                try
                {
                    String data = (String) support.getTransferable ().getTransferData (DataFlavor.stringFlavor);
                    if (data.contains ("@")) return list.getTransferHandler ().importData (support);  // indicates BibTeX format
                    return super.importData (support);  // Base class will reject serialized N2A objects
                }
                catch (IOException | UnsupportedFlavorException e)
                {
                    return false;
                }
            }
        });


        Lay.BLtg (this,
            "N", Lay.BL ("C", textQuery, "eb=2"),
            "C", Lay.sp (list)
        );

        search ();  // This will safely block until the models dir is loaded. If that takes too long for comfort, other arrangements are possible.
    }

    public void search ()
    {
        if (thread != null) thread.stop = true;

        String query = textQuery.getText ();
        thread = new SearchThread (query.trim ());
        thread.start ();
    }

    public void selectCurrent ()
    {
        int index = list.getSelectedIndex ();
        if (index >= 0)
        {
            MNode doc = model.get (index);
            PanelReference.instance.panelMRU.useDoc (doc);
            recordSelected (doc);
        }
    }

    public static void recordSelected (final MNode doc)
    {
        EventQueue.invokeLater (new Runnable ()
        {
            public void run ()
            {
                PanelReference mep = PanelReference.instance;
                mep.panelEntry.model.setRecord (doc);
                mep.panelEntry.table.requestFocusInWindow ();
            }
        });
    }

    public void hideSelection ()
    {
        int index = list.getSelectedIndex ();
        if (index >= 0) lastSelection = index;
        list.clearSelection ();
    }

    public void showSelection ()
    {
        if (list.getSelectedIndex () < 0)
        {
            int last = model.getSize () - 1;
            if      (lastSelection < 0   ) list.setSelectedIndex (0);
            else if (lastSelection > last) list.setSelectedIndex (last);
            else                           list.setSelectedIndex (lastSelection);
        }
    }

    public String currentKey ()
    {
        int index = list.getSelectedIndex ();
        if (index < 0) return "";
        return model.get (index).key ();
    }

    public String keyAfter (MNode doc)
    {
        int index = model.indexOf (doc);
        if (index < 0  ||  index == model.getSize () - 1) return "";  // indexOf(String) will return end-of-list in response to this value.
        return model.get (index + 1).key ();
    }

    public int indexOf (String key)
    {
        int count = model.size ();
        if (key.isEmpty ()) return count;
        for (int i = 0; i < count; i++) if (model.get (i).key ().equals (key)) return i;
        return -1;
    }

    public void removeDoc (String key)
    {
        int index = indexOf (key);
        if (index < 0) return;
        model.remove (index);
        if (lastSelection > index) lastSelection--;
        lastSelection = Math.min (model.size () - 1, lastSelection);
    }

    /**
        For multirepo, if the key of the doc in a Holder gets claimed by another doc,
        then the Holder should be updated to point to the new doc. This could, for example,
        change what color it gets displayed as.
    **/
    public void updateDoc (MNode doc)
    {
        String key = doc.key ();
        int index = indexOf (key);
        if (index < 0) return;
        MNode n = model.get (index);
        if (n == doc) return;
        model.setElementAt (n, index);
    }

    public void insertNextAt (int at)
    {
        insertAt = at;
    }

    public void insertDoc (MNode doc)
    {
        int index = model.indexOf (doc);
        if (index < 0)
        {
            if (insertAt > model.size ()) insertAt = 0;  // The list has changed, perhaps due to filtering, and our position is no longer valid, so simply insert at top.
            model.add (insertAt, doc);
            lastSelection = insertAt;
        }
        else
        {
            lastSelection = index;
        }
        insertAt = 0;
    }

    public void updateUI ()
    {
        super.updateUI ();
        if (renderer != null) renderer.updateUI ();
    }

    // Retrieve records matching the filter text, and deliver them to the model.
    public class SearchThread extends Thread
    {
        public String query;
        public boolean stop;

        public SearchThread (String query)
        {
            this.query = query.toLowerCase ();
        }

        @Override
        public void run ()
        {
            List<MNode> results = new LinkedList<MNode> ();
            for (MNode i : AppData.references)
            {
                if (stop) return;
                if (i.key ().toLowerCase ().contains (query)) results.add (i);
            }

            // Update of list should be atomic with respect to other ui events.
            EventQueue.invokeLater (new Runnable ()
            {
                public void run ()
                {
                    synchronized (model)
                    {
                        if (stop) return;
                        model.clear ();
                        for (MNode record : results)
                        {
                            if (stop) return;
                            model.addElement (record);
                        }
                    }
                }
            });
        }
    }
    protected SearchThread thread;

    public static class MNodeRenderer extends JTextField implements ListCellRenderer<MNode>
    {
        protected static DefaultHighlighter.DefaultHighlightPainter painter;

        public MNodeRenderer ()
        {
            painter = new DefaultHighlighter.DefaultHighlightPainter (UIManager.getColor ("List.selectionBackground"));
            setBorder (new EmptyBorder (0, 0, 0, 0));
        }

        public Component getListCellRendererComponent (JList<? extends MNode> list, MNode doc, int index, boolean isSelected, boolean cellHasFocus)
        {
            String name = doc.get ("title");
            if (name.isEmpty ()) name = doc.key ();
            setText (name);

            Color color = Color.black;
            if (! AppData.references.isWriteable (doc))
            {
                String colorName = "";
                MNode repo = AppData.repos.child (doc.parent ().key ());  // This can return null if multirepo structure changes and this panel is repainted before the change notification arrives.
                if (repo != null) colorName = repo.get ("color");
                if (! colorName.isEmpty ())
                {
                    try {color = Color.decode (colorName);}
                    catch (NumberFormatException e) {}
                }
                if (color.equals (Color.black)) color = Color.blue;
            }
            setForeground (color);

            if (isSelected)
            {
                Highlighter h = getHighlighter ();
                h.removeAllHighlights ();
                try
                {
                    h.addHighlight (0, name.length (), painter);
                }
                catch (BadLocationException e)
                {
                }
            }

            return this;
        }

        public void updateUI ()
        {
            super.updateUI ();
            painter = new DefaultHighlighter.DefaultHighlightPainter (UIManager.getColor ("List.selectionBackground"));
        }
    }
}
