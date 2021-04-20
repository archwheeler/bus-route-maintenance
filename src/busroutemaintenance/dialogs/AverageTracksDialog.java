package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class AverageTracksDialog extends BasicDialog {

  public AverageTracksDialog(String layerName) {
    super(tr("Average GPX tracks"));
    
    setContent(String.format(
        tr("Do you want to average the GPX tracks in the active layer (''%s'')?"),
            layerName));
    
    showDialog();
  }

}
