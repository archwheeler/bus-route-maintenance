package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Shortcut;

import busroutemaintenance.dialogs.MaintenanceDialog;

@SuppressWarnings("serial")
public class MaintenanceAction extends JosmActiveLayerAction {

  public MaintenanceAction() {
    super(tr("Bus route maintenance"), "maintenance", tr("Bus route maintenance"),
        Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
        tr("Bus route maintenance")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    Layer activeLayer = getActiveLayer();
    if (activeLayer == null) {
      noActiveLayerError();
      return;
    }
    
    MaintenanceDialog dlg = new MaintenanceDialog(activeLayer.getName());
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      GpxData activeData = getActiveData(activeLayer);
      if (activeData.getTrackCount() != 1) {
        GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
            tr("Expected a single track"), tr("Error"), JOptionPane.WARNING_MESSAGE));
        return;
      }
    }
    
    return;
  }

}
