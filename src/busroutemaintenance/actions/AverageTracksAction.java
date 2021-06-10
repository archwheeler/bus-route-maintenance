package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Shortcut;

import busroutemaintenance.Utils;
import busroutemaintenance.average.AverageAlgorithm;
import busroutemaintenance.average.BasicAverage;
import busroutemaintenance.average.ExperimentalAverage;
import busroutemaintenance.dialogs.AverageTracksDialog;

@SuppressWarnings("serial")
public class AverageTracksAction extends BasicAction {
  
  private static Color AVERAGE_COLOUR = Color.green;

  public AverageTracksAction() {
    super(tr("Average GPS trajectories"), "average", tr("Average GPS trajectories"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Average GPS trajectories")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    Layer activeLayer = getActiveLayer();
    if (activeLayer == null) {
      noActiveLayerError();
      return;
    }
    
    AverageTracksDialog dlg = new AverageTracksDialog(activeLayer.getName());
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      GpxData activeData = getActiveData(activeLayer);
      
      try {
        AverageAlgorithm algorithm;
        if (dlg.isBasic())
          algorithm = new BasicAverage();
        else
          algorithm = new ExperimentalAverage();
        GpxLayer averageLayer = new GpxLayer(algorithm.averageTracks(activeData),
            String.format("%s AVG", activeLayer.getName()));
        averageLayer.setColor(AVERAGE_COLOUR);
        layerManager.addLayer(averageLayer);
      } catch (IllegalDataException e) {
        Utils.displayError(e.getMessage());
      }
    }
    
    return;
  }

}
