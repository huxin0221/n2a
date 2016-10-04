/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a;

import gov.sandia.umf.platform.ui.images.ImageUtil;
import gov.sandia.umf.platform.plugins.extpoints.ProductCustomization;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

public class N2AProductCustomization implements ProductCustomization
{
    public String getProductLongName ()
    {
        return "Neurons to Algorithms";
    }

    public String getProductShortName ()
    {
        return "N2A";
    }

    public String getProductVersion ()
    {
        return N2APlugin.getInstance ().getVersion ();
    }

    public String getProductDevelopedBy ()
    {
        return "Fred Rothganger (PI), Derek Trumbo, Christy Warrender, Brad Aimone, Corinne Teeter, Brandon Rohrer, Steve Verzi, Ann Speed, Asmeret Bier, Felix Wang";
    }

    public String getCopyright ()
    {
        return "Copyright &copy; 2013 Sandia Corporation. Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation, the U.S. Government retains certain rights in this software.";
    }

    public String getLicense ()
    {
        return "This software is released under the BSD license.  Please refer to the license information provided in this distribution for the complete text.";
    }

    public String getSupportEmail ()
    {
        return "n2a@sandia.gov";
    }

    public List<Image> getWindowIcons ()
    {
        ArrayList<Image> result = new ArrayList<Image> ();
        result.add (ImageUtil.getImage ("n2a-16.png").getImage ());
        result.add (ImageUtil.getImage ("n2a-32.png").getImage ());
        return result;
    }

    public ImageIcon getAboutImage ()
    {
        return ImageUtil.getImage ("n2a-splash.png");
    }
}
