/*
Copyright 2013-2018 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
Under the terms of Contract DE-NA0003525 with NTESS,
the U.S. Government retains certain rights in this software.
*/

package gov.sandia.n2a.ui.jobs;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
    Create a spike-raster plot.
    The parsing functions in this class are similar to Plot, but they can be implemented
    more efficiently because we have simpler needs.
**/
public class Raster
{
    public XYSeriesCollection dataset;
    public List<Integer> columns = new ArrayList<Integer> ();
    public int nextColumn = -1;

    public Raster (Path path)
    {
    	parsePrnFile (path);
    }

    public JPanel createGraphPanel ()
    {
        JFreeChart chart = createChart (dataset);
        return new ChartPanelDrag (chart);
    }

    public void parsePrnFile (Path f)
    {
        dataset = new XYSeriesCollection ();
        XYSeries series = new XYSeries ("Spikes");
        dataset.addSeries (series);

        try
        {
            int row = 0;
            int timeColumn = -1;  // It's possible that there might not be a time column. In that case, we use raw row index;

            BufferedReader br = Files.newBufferedReader (f);
            while (true)
            {
                String line = br.readLine ();
                if (line == null) break;  // indicates end of stream

                line = line.trim ();
                if (line.length () == 0) continue;
                if (line.startsWith ("End of")) continue;

                String[] parts = line.split ("\t");  // TODO: does Xyce output tabs or spaces? May have to switch regexp here, depending on source.

                char firstCharacter = parts[0].charAt (0);
                if (firstCharacter < '0'  ||  firstCharacter > '9')  // column header
                {
                    if (timeColumn < 0)
                    {
                        int timeMatch = 0;  // goodness of match
                        for (int p = 0; p < parts.length; p++)
                        {
                            int potentialMatch = 0;
                            String columnName = parts[p];
                            if      (columnName.equals ("TIME")) potentialMatch = 1;
                            else if (columnName.equals ("$t"  )) potentialMatch = 2;
                            if (potentialMatch > timeMatch)
                            {
                                timeMatch = potentialMatch;
                                timeColumn = p;
                            }
                        }
                    }

                    for (int p = columns.size (); p <= timeColumn; p++) columns.add (p, 0);  // These should never be accessed.

                    for (int p = columns.size (); p < parts.length; p++)
                    {
                        try
                        {
                            int c = Integer.parseInt (parts[p]);
                            if (c < 0) throw new NumberFormatException ();
                            columns.add (p, c);
                        }
                        catch (NumberFormatException e)
                        {
                            columns.add (p, nextColumn--);
                        }
                    }

                    continue;
                }

                // If for some reason we don't have column headers, then auto-assign them.
                // It is unlikely that column headers will show up later if they don't come first.
                for (int p = columns.size (); p < parts.length; p++) columns.add (p, nextColumn--);

                int p = timeColumn;
                double time = row;
                if (p >= 0) time = Double.parseDouble (parts[timeColumn]);
                p++;
                for (; p < parts.length; p++)
                {
                    if (! parts[p].isEmpty ()  &&  Double.parseDouble (parts[p]) != 0) series.add (time, columns.get (p));
                }
                row++;
            }
            br.close ();
        }
        catch (IOException e)
        {
        }
    }

    @SuppressWarnings("serial")
    public JFreeChart createChart (final XYDataset dataset)
    {
        final JFreeChart chart = ChartFactory.createScatterPlot
        (
            null,                     // chart title
            null,                     // x axis label
            null,                     // y axis label
            dataset,                  // data
            PlotOrientation.VERTICAL,
            false,                    // include legend
            true,                     // tooltips
            false                     // urls
        );

        XYPlot plot = chart.getXYPlot ();
        plot.setBackgroundPaint    (Color.white);
        plot.setRangeGridlinePaint (Color.lightGray);
        plot.setDomainPannable (true);
        plot.setRangePannable  (true);

        plot.setRenderer (new XYDotRenderer ()
        {
            public void drawItem (java.awt.Graphics2D g2, XYItemRendererState state, java.awt.geom.Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass)
            {
                // Copied from org.jfree.chart.renderer.xy.XYDotRenderer.java and modified.
                // This would only need to be a couple of lines if they authors of jfreechart had not made dotWidth and dotHeight private members.
                // Yet another example of textbook OO programming gone awry. (Can anyone hear me scream?)

                if (! getItemVisible (series, item)) return;

                int dotWidth  = 1;

                double rasterLines = rangeAxis.getRange ().getLength ();
                int    pixels      = g2.getClipBounds ().height;
                double height = pixels / rasterLines;
                if      (height > 10) height -= 2;
                else if (height > 2 ) height -= 1;
                int dotHeight = (int) Math.min (20, Math.max (1, Math.floor (height)));

                double x = dataset.getXValue (series, item);
                double y = dataset.getYValue (series, item);
                if (Double.isNaN (y)) return;
                double adjx = (dotWidth  - 1) / 2.0;
                double adjy = (dotHeight - 1) / 2.0;

                RectangleEdge xAxisLocation = plot.getDomainAxisEdge ();
                RectangleEdge yAxisLocation = plot.getRangeAxisEdge  ();
                double transX = domainAxis.valueToJava2D (x, dataArea, xAxisLocation) - adjx;
                double transY = rangeAxis .valueToJava2D (y, dataArea, yAxisLocation) - adjy;

                g2.setPaint (Color.black);
                PlotOrientation orientation = plot.getOrientation ();
                if (orientation == PlotOrientation.HORIZONTAL) g2.fillRect ((int) transY, (int) transX, dotHeight, dotWidth);
                else                                           g2.fillRect ((int) transX, (int) transY, dotWidth,  dotHeight);

                int datasetIndex = plot.indexOf (dataset);
                updateCrosshairValues (crosshairState, x, y, datasetIndex, transX, transY, orientation);
            }
        });

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis ();
        rangeAxis.setStandardTickUnits (NumberAxis.createIntegerTickUnits ());  // Integer units only

        return chart;
    }
}
