package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class MaintenanceDialog extends BasicDialog {

  public MaintenanceDialog(String layerName) {
    super(tr("Bus route maintenance"));
    
    setContent(String.format(
        tr("Do you want to perform bus route maintenance using the GPX track in the active layer "
            + "(''%s'')?"), layerName));
    
    showDialog();
  }

}