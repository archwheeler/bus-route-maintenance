package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;

public class SiriCSVImporter extends FileImporter {

  public static final String SIRI_FILE_EXT = "csv";
  public static final String SIRI_FILE_EXT_DOT = "." + SIRI_FILE_EXT;
  
  public SiriCSVImporter() {
    super(new ExtensionFileFilter(SIRI_FILE_EXT, SIRI_FILE_EXT,
          tr("SIRI CSV Files") + " (*" + SIRI_FILE_EXT_DOT + ")"));
  }
  
}
