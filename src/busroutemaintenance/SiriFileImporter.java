package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;

public class SiriFileImporter extends FileImporter {

  public static final String SIRI_FILE_EXT = "siri";
  public static final String SIRI_FILE_EXT_DOT = "." + SIRI_FILE_EXT;
  
  public SiriFileImporter() {
    super(new ExtensionFileFilter(SIRI_FILE_EXT, SIRI_FILE_EXT,
          tr("SIRI Files") + " (*" + SIRI_FILE_EXT_DOT + ")"));
  }
  
  @Override
  public boolean acceptFile(File pathname) {
    return super.acceptFile(pathname) && SiriFileReader.isSiriFile(pathname);
  }
  
}
