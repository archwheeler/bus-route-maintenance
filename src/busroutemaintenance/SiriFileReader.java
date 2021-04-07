package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

public class SiriFileReader {
  
  private static int SIGNATURE = 0x53495249;
  private static int SIGNATURE_SIZE = 8;
  private static int BUFFER_SIZE = 24;
  
  private File file;
  private ProgressMonitor monitor;
  
  public SiriFileReader(File file) {
    this.file = file;
  }
  
  public SiriFileReader(File file, ProgressMonitor monitor) {
    this.file = file;
    this.monitor = monitor;
  }
  
  public static boolean isSiriFile(File file) {
    try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {
      int signature = is.readInt();
      return signature == SIGNATURE;
    } catch (Exception e) {
      return false;
    }
  }
  
  public void setProgressMonitor(ProgressMonitor monitor) {
    this.monitor = monitor;
    return;
  }
  
  public ProgressMonitor getProgressMonitor() {
    return this.monitor;
  }

  public GpxData toGpx() throws IllegalDataException {
    if (file == null || !isSiriFile(file))
      throw new IllegalDataException(tr("File is not a SIRI file"));
    
    GpxData gpxData = new GpxData();
    try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
      // Ignore the header information
      is.skip(SIGNATURE_SIZE);
      
      WayPoint current;
      List<WayPoint> waypoints = new ArrayList<WayPoint>();
      long time;
      double lat;
      double lon;
      ByteBuffer buffer;
      byte[] bytes = new byte[BUFFER_SIZE];
      while(is.read(bytes) == BUFFER_SIZE) {
        buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        time = (long) buffer.getDouble();
        lat = buffer.getDouble();
        lon = buffer.getDouble();
        
        current = new WayPoint(new LatLon(lat, lon));
        current.setTimeInMillis(time);
        
        waypoints.add(current);
        gpxData.addWaypoint(current);
        
        if (monitor != null)
          monitor.worked(1);
      }
      // Sort the waypoints by time
      Collections.sort(waypoints, new Comparator<WayPoint>() {
        public int compare(WayPoint w1, WayPoint w2) {
          double time1 = w1.getTime();
          double time2 = w2.getTime();
          
          if (time1 == time2)
            return 0;
          return time1 < time2 ? -1 : 1;
        }
      });
      
      List<WayPoint> segment = new ArrayList<WayPoint>();
      for (WayPoint wpt : waypoints) {
        segment.add(wpt);
      }
      
      List<IGpxTrackSegment> segments = new ArrayList<IGpxTrackSegment>();
      segments.add(new GpxTrackSegment(segment));
      gpxData.addTrack(new GpxTrack(segments, Collections.<String, Object>emptyMap()));
      
    } catch (Exception e) {
      throw new IllegalDataException(tr("Could not convert SIRI file to GPX"));
    }
    return gpxData;
  }
  
}
