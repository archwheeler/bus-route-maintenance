package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

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
  
  @Override
  public void importData(File file, ProgressMonitor monitor) {
    try {
      monitor.beginTask(String.format(tr("Importing SIRI file ''%s''..."), file.getName()));
      
      SiriFileReader r = new SiriFileReader(file, monitor);
      GpxData gpxData = r.toGpx();
      
      GpxLayer gpxLayer = new GpxLayer(gpxData, file.getName());
      MainApplication.getLayerManager().addLayer(gpxLayer);
      monitor.worked(1);
    } catch (IllegalDataException e) {
      Utils.displayError(String.format(tr("Error loading SIRI file ''%s'':\n%s"), file.getName(),
          e.getMessage()));
    } finally {
      monitor.finishTask();
    }
  }
  
}
