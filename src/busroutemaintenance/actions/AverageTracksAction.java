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

import busroutemaintenance.AverageTracksAlgorithm;
import busroutemaintenance.DivideConquerAverageTracks;
import busroutemaintenance.Utils;
import busroutemaintenance.dialogs.AverageTracksDialog;

@SuppressWarnings("serial")
public class AverageTracksAction extends JosmActiveLayerAction {
  
  private static Color AVERAGE_COLOUR = Color.green;
  private AverageTracksAlgorithm averageTracksAlgorithm = new DivideConquerAverageTracks();

  public AverageTracksAction() {
    super(tr("Average GPX tracks"), "average", tr("Average GPX tracks"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Average GPX tracks")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
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
        GpxLayer averageLayer = new GpxLayer(averageTracksAlgorithm.averageTracks(activeData),
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
