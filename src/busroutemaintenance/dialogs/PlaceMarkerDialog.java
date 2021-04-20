package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class PlaceMarkerDialog extends BasicDialog {

  public PlaceMarkerDialog(String position) {
    super("Place marker");
    setContent(String.format(tr("Please place a marker at the %s of the GPX track."), position));
    showDialog();
  }

}
