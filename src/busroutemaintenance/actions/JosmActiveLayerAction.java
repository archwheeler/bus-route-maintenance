package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public abstract class JosmActiveLayerAction extends JosmAction {
  
  public static MainLayerManager layerManager = MainApplication.getLayerManager();
  
  public JosmActiveLayerAction(String tr, String string, String tr2, Shortcut registerShortcut,
      boolean b) {
    super(tr, string, tr2, registerShortcut, b);
  }

  public void noActiveLayerError() {
    GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
        tr("No active layer found."), tr("Error"),
        JOptionPane.WARNING_MESSAGE));
  }
  
  public Layer getActiveLayer() {
    return layerManager.getActiveLayer();
  }
  
  public GpxData getActiveData(Layer activeLayer) {
    GpxData activeData;
    try {
      activeData = (GpxData) ((GpxLayer) activeLayer).getData();
    } catch (Exception e) {
      GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
          tr("Error loading GPX data from the active layer."), tr("Error"),
          JOptionPane.WARNING_MESSAGE));
      return null;
    }
    return activeData;
  }
}
