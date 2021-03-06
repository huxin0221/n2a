/*
Copyright 2013 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.umf.platform.ui.ensemble.run;

import gov.sandia.n2a.plugins.extpoints.Backend;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import replete.event.ChangeNotifier;
import replete.gui.controls.WComboBox;
import replete.util.Lay;

public class SimulatorSelectionPanel extends JPanel
{
    private JPanel pnlCenter;
    private JComboBox cboSimulators;
    private DefaultComboBoxModel mdlSimulators;

    public SimulatorSelectionPanel(CreateRunEnsembleDialog parentRef, Backend[] simulators, Backend defaultSimulator)
    {
        // Populate simulator combo box model.
        mdlSimulators = new DefaultComboBoxModel();
        for(Backend simulator : simulators) {
            mdlSimulators.addElement(new SimulatorWrapper(simulator));
        }

        // Set up combo box.
        cboSimulators = new WComboBox(mdlSimulators);
        if(defaultSimulator != null && mdlSimulators.getIndexOf(defaultSimulator) != -1) {
            cboSimulators.setSelectedItem(defaultSimulator);
        } else if(simulators.length != 0) {
            cboSimulators.setSelectedItem(simulators[0]);
        }
        cboSimulators.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fireSimulatorChangeNotifier();
            }
        });

        Lay.BLtg(this,

            "N", Lay.BL(
                "W", Lay.FL("L",
                    Lay.hn(HelpLabels.createLabelPanel(parentRef, "Simulator", "part-name")),
                    cboSimulators),
                "C", Lay.lb(" ")
            )
        );
    }

    public Backend getSimulator() {
        return ((SimulatorWrapper) cboSimulators.getSelectedItem()).simulator;
    }


    //////////////
    // NOTIFIER //
    //////////////

    private ChangeNotifier simulatorChangedNotifier = new ChangeNotifier(this);
    public void addSimulatorChangeListener(ChangeListener listener) {
        simulatorChangedNotifier.addListener(listener);
    }
    public void fireSimulatorChangeNotifier() {
        simulatorChangedNotifier.fireStateChanged();
    }


    /////////////////
    // INNER CLASS //
    /////////////////

    public class SimulatorWrapper {
        public Backend simulator;
        public SimulatorWrapper(Backend sim) {
            simulator = sim;
        }
        @Override
        public String toString() {
            return simulator.getName();
        }
    }
}
