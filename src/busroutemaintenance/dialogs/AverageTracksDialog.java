package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class AverageTracksDialog extends BasicDialog {

  public AverageTracksDialog(String layerName) {
    super(tr("Average GPS trajectories"));
    
    setContent(String.format(
        tr("Do you want to average the GPS trajectories in the active layer (''%s'')?"),
            layerName));
    
    showDialog();
  }

}
