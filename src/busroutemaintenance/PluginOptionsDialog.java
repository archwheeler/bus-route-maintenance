package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class PluginOptionsDialog extends BasicDialog {

  public PluginOptionsDialog() {
    super(tr("Bus Route Maintenance options"));
    setContent(tr("Options"));
    
    showDialog();
  }

}
