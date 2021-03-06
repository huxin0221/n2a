/*
Copyright 2013-2019 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a;

import gov.sandia.n2a.db.AppData;
import gov.sandia.n2a.plugins.PluginManager;
import gov.sandia.n2a.ui.MainFrame;
import gov.sandia.n2a.ui.settings.SettingsLookAndFeel;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class Main
{
    public static void main (String[] args)
    {
        setUncaughtExceptionHandler (null);

        // Set global application properties.
        AppData.properties.set ("name",         "Neurons to Algorithms");
        AppData.properties.set ("abbreviation", "N2A");
        AppData.properties.set ("version",      "1.0");
        AppData.checkInitialDB ();

        // Load plugins
        ArrayList<String> pluginClassNames = new ArrayList<String> ();
        pluginClassNames.add ("gov.sandia.n2a.backend.internal.InternalPlugin");
        pluginClassNames.add ("gov.sandia.n2a.backend.xyce.XycePlugin");
        pluginClassNames.add ("gov.sandia.n2a.backend.c.PluginC");
        pluginClassNames.add ("gov.sandia.n2a.backend.neuroml.PluginNeuroML");
        pluginClassNames.add ("gov.sandia.n2a.backend.neuron.PluginNeuron");

        ArrayList<File> pluginDirs = new ArrayList<File> ();
        pluginDirs.add (new File (AppData.properties.get ("resourceDir"), "plugins"));

        for (String arg : args)
        {
            if (arg.startsWith ("plugin="   )) pluginClassNames.add           (arg.substring (7));
            if (arg.startsWith ("pluginDir=")) pluginDirs      .add (new File (arg.substring (10)));
        }

        if (! PluginManager.initialize (new N2APlugin (), pluginClassNames.toArray (new String[0]), pluginDirs.toArray (new File[0])))
        {
            System.err.println (PluginManager.getInitializationErrors ());
        }

        // Create the main frame.
        EventQueue.invokeLater (new Runnable ()
        {
            public void run ()
            {
                SettingsLookAndFeel.instance.load ();  // Note that SettingsLookAndFeel is instantiated when the plugin system is initialized above.
                new MainFrame ();
                setUncaughtExceptionHandler (MainFrame.instance);
            }
        });
    }

    public static void setUncaughtExceptionHandler (final JFrame parent)
    {
        Thread.setDefaultUncaughtExceptionHandler (new UncaughtExceptionHandler ()
        {
            public void uncaughtException (Thread thread, final Throwable throwable)
            {
                File crashdump = new File (AppData.properties.get ("resourceDir"), "crashdump");
                try
                {
                    PrintStream err = new PrintStream (crashdump);
                    throwable.printStackTrace (err);
                    err.close ();
                }
                catch (FileNotFoundException e) {}

                JOptionPane.showMessageDialog
                (
                    parent,
                    "<html><body><p style='width:300px'>Our apologies, but an exception was raised.<br>"
                    + "<br>Details have been saved in " + crashdump.getAbsolutePath () + "<br>"
                    + "<br>Please consider filing a bug report on github, including the crashdump file and a description of what you were doing at the time.</p></body></html>",
                    "Internal Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
