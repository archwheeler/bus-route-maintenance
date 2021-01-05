package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class ImportSiriDataDialog extends BasicDialog {

  public ImportSiriDataDialog() {
    super(tr("Import SIRI data"));
    setContent(tr("Import SIRI data?"));
    
    showDialog();
  }

}
