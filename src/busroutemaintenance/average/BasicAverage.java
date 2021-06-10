package busroutemaintenance.average;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.IllegalDataException;

public class BasicAverage implements AverageAlgorithm {
  
  private static double MAX_DISTANCE = 1E-5;
  
  private static WayPoint projectWayPointOnSegment(WayPoint point, IGpxTrackSegment segment) {
    List<WayPoint> points = new ArrayList<WayPoint>(segment.getWayPoints());
    int totalPoints = points.size();
    if (totalPoints == 0)
      return null;
    if (totalPoints == 1)
      return new WayPoint(points.get(0));
    
    LatLon pointCoor = point.getCoor();
    double lat = point.lat();
    double lon = point.lon();
    WayPoint b = points.get(0);
    double bLat = b.lat();
    double bLon = b.lon();
    WayPoint projection = null;
    double minDistance = Double.MAX_VALUE;
    for (int i = 1; i != totalPoints; ++i) {
      // Get the next line segment a->b
      WayPoint a = b;
      Double aLat = bLat;
      Double aLon = bLon;
      b = points.get(i);
      bLat = b.lat();
      bLon = b.lon();
      
      // Project the point onto the line a->b
      double k = ((bLon-aLon)*(lat-aLat) - (bLat-aLat)*(lon-aLon)) /
          (Math.pow(bLon-aLon, 2) + Math.pow(bLat-aLat, 2));
      LatLon projCoor = new LatLon(lat - k*(bLon-aLon), lon + k*(bLat-aLat));
      
      // If the projection isn't on the line segment a->b, set it to the closest out of a and b
      if (projCoor.lat() < Math.min(aLat, bLat) || projCoor.lat() > Math.max(aLat, bLat)) {
        if (pointCoor.distanceSq(a.getCoor()) <= pointCoor.distanceSq(b.getCoor())) {
          projCoor = a.getCoor();
        } else {
          projCoor = b.getCoor();
        }
      }
      
      // If this projection is the closest found so far, update our projection
      double distance = projCoor.distanceSq(pointCoor);
      if (distance < minDistance) {
        minDistance = distance;
        projection = new WayPoint(projCoor);
      }
    }
    
    // Only return a projection if it's reasonably close to our original point
    return (minDistance < MAX_DISTANCE) ? projection : null;
  }

  @Override
  public GpxData averageTracks(GpxData data) throws IllegalDataException {
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

}