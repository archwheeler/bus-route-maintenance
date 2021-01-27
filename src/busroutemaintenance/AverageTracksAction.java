package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class AverageTracksAction extends JosmAction {
  
  private static Color AVERAGE_COLOUR = Color.green;
  private static double MAX_DISTANCE = 1E-6;

  public AverageTracksAction() {
    super(tr("Average GPX tracks"), "average", tr("Average GPX tracks"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Average GPX tracks")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }
  
  private static WayPoint projectWayPointOnSegment(WayPoint point, IGpxTrackSegment segment) {
    List<WayPoint> points = new ArrayList<WayPoint>(segment.getWayPoints());
    int totalPoints = points.size();
    if (totalPoints == 0)
      return null;
    if (totalPoints == 1)
      return new WayPoint(points.get(0));
    
    double lat = point.lat();
    double lon = point.lon();
    WayPoint a;
    WayPoint b = points.get(0);
    double alat;
    double alon;
    double blat = b.lat();
    double blon = b.lon();
    WayPoint projection = null;
    LatLon current;
    double k;
    double minDistance = Double.MAX_VALUE;
    double distance;
    LatLon pointCoor = point.getCoor();
    for (int i = 1; i != totalPoints; ++i) {
      a = b;
      alat = blat;
      alon = blon;
      b = points.get(i);
      blat = b.lat();
      blon = b.lon();
      
      k = ((blon-alon)*(lat-alat) - (blat-alat)*(lon-alon)) /
          (Math.pow(blon-alon, 2) + Math.pow(blat-alat, 2));
      current = new LatLon(lat - k*(blon-alon), lon + k*(blat-alat));
      
      if (current.lat() < Math.min(alat, blat) || current.lat() > Math.max(alat, blat)) {
        if (pointCoor.distanceSq(a.getCoor()) <= pointCoor.distanceSq(b.getCoor())) {
          current = a.getCoor();
        } else {
          current = b.getCoor();
        }
      }
      
      distance = current.distanceSq(pointCoor);
      if (distance < minDistance) {
        minDistance = distance;
        projection = new WayPoint(current);
      }
    }
    
    if (minDistance < MAX_DISTANCE)
      return projection;
    else
      return null;
  }
  
  private static GpxData averageTracks(GpxData data) throws IllegalDataException {
    if (data == null)
      throw new IllegalDataException(tr("Layer contains no GPX data"));
    if (data.getTrackSegsCount() < 2)
      throw new IllegalDataException(tr("Not enough segments to compute average"));
    
    // Our base track is the track with the longest distance
    IGpxTrackSegment base = data.getTrackSegmentsStream().max(new Comparator<IGpxTrackSegment>() {
      public int compare(IGpxTrackSegment t1, IGpxTrackSegment t2) {
        double len1 = t1.length();
        double len2 = t2.length();
        
        if (len1 == len2)
          return 0;
        return len1 < len2 ? -1 : 1;
      }
    }).get();
    
    // Ignore segments with no WayPoints
    List<IGpxTrackSegment> segments = data.getTrackSegmentsStream()
                                          .filter(s -> s.length() > 0)
                                          .collect(Collectors.toList());
    
    // Create an averageSegment containing the average of the closest points to each point in base
    double totalSegments;
    double totalLat;
    double totalLon;
    WayPoint closest;
    List<WayPoint> averageSegment = new ArrayList<WayPoint>();
    for (WayPoint point : base.getWayPoints()) {
      totalLat = 0.0;
      totalLon = 0.0;
      totalSegments = 0.0;
      for (IGpxTrackSegment segment : segments) {
        closest = projectWayPointOnSegment(point, segment);
        if (closest != null) {
          totalLat += closest.lat();
          totalLon += closest.lon();
          totalSegments += 1.0;
        }
      }
      averageSegment.add(new WayPoint(new LatLon(totalLat/totalSegments, totalLon/totalSegments)));
    }
    
    // Return new GPX data containing only the new average segment
    GpxData average = new GpxData();
    List<IGpxTrackSegment> averageSegments = new ArrayList<IGpxTrackSegment>();
    averageSegments.add(new GpxTrackSegment(averageSegment));
    average.addTrack(new GpxTrack(averageSegments, Collections.<String, Object>emptyMap()));
    return average;
  }
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    AverageTracksDialog dlg = new AverageTracksDialog();
    MainLayerManager layerManager = MainApplication.getLayerManager();
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      GpxLayer currentLayer;
      GpxData currentData;
      try {
        currentLayer = (GpxLayer) layerManager.getActiveLayer();
        currentData = (GpxData) currentLayer.getData();
      } catch (Exception e) {
        GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
            tr("Error loading GPX data from current layer."), tr("Error"),
            JOptionPane.WARNING_MESSAGE));
        return;
      }
      
      try {
        GpxLayer averageLayer = new GpxLayer(averageTracks(currentData),
            String.format("%s AVG", currentLayer.getName()));
        averageLayer.setColor(AVERAGE_COLOUR);
        layerManager.addLayer(averageLayer);
      } catch (IllegalDataException e) {
        GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
            String.format("%s", e.getMessage()), tr("Error"), JOptionPane.WARNING_MESSAGE));
      }
    }
    
    return;
  }

}
