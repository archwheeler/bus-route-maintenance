package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class SegmentTracksDialog extends BasicDialog {

  public SegmentTracksDialog(String layerName) {
    super(tr("Segment GPX tracks"));
    
    setContent(String.format(
        tr("Do you want to segment the GPX track in the active layer (''%s'')?"),
            layerName));
    
    showDialog();
  }

}