package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class AverageTracksDialog extends BasicDialog {

  public AverageTracksDialog() {
    super(tr("Average GPX tracks"));
    setContent(tr("Do you want to average the GPX tracks in the current layer?"));
    
    showDialog();
  }

}
