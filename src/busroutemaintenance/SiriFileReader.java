package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

public class SiriFileReader {
  
  private static int SIGNATURE = 0x53495249;
  private static int SIGNATURE_SIZE = 8;
  private static int BUFFER_SIZE = 24;
  
  private File file;
  
  public SiriFileReader(File file) {
    this.file = file;
  }
  
  public static boolean isSiriFile(File file) {
    try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {
      int signature = is.readInt();
      return signature == SIGNATURE;
    } catch (Exception e) {
      return false;
    }
  }

  public GpxData toGpx(ProgressMonitor monitor) throws IllegalDataException {
    if (file == null || !isSiriFile(file))
      throw new IllegalDataException(tr("File is not a SIRI file"));
    
    GpxData gpxData = new GpxData();
    try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
      // Ignore the header information
      is.skip(SIGNATURE_SIZE);
      
      WayPoint wpt;
      long time;
      ByteBuffer buffer;
      byte[] bytes = new byte[BUFFER_SIZE];
      while(is.read(bytes) == BUFFER_SIZE) {
        buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        time = (long) buffer.getDouble();
        wpt = new WayPoint(new LatLon(buffer.getDouble(), buffer.getDouble()));
        wpt.setTimeInMillis(time);
        gpxData.addWaypoint(wpt);
        monitor.worked(1);
      }
    } catch (Exception e) {
      throw new IllegalDataException(tr("Could not convert SIRI file to GPX"));
    }
    return gpxData;
  }
  
}
