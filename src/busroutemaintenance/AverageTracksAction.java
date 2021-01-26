package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class AverageTracksAction extends JosmAction {

  public AverageTracksAction() {
    super(tr("Average GPX tracks"), "average", tr("Average GPX tracks"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Average GPX tracks")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    AverageTracksDialog dlg = new AverageTracksDialog();
    MainLayerManager layerManager = MainApplication.getLayerManager();
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      GpxLayer currentLayer;
      GpxData currentData;
      try {
        currentLayer = (GpxLayer) layerManager.getActiveLayer();
        currentData = (GpxData) currentLayer.getData();
      } catch (Exception e) {
        GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
            tr("Error loading GPX data from current layer."), tr("Error"),
            JOptionPane.WARNING_MESSAGE));
        return;
      }
      
      System.out.println(currentData.getTrackSegsCount());
    }
  }

}
