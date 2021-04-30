package busroutemaintenance;

import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

public class Match {
  
  private static final double DISTANCE_THRESHOLD = 30;
  private static final double BEARING_THRESHOLD = Math.PI / 9.0;
  
  private Way way;
  private List<Node> nodes;
  
  public Match(Way way) {
    this.way = way;
    this.nodes = way.getNodes();
  }
  
  private LatLon projectPointBetweenPoints(LatLon point, Node a, Node b) {
    double lat = point.lat();
    double aLat = a.lat();
    double bLat = b.lat();
    
    double lon = point.lon();
    double aLon = a.lon();
    double bLon = b.lon();
    // Project the point onto the line a->b
    double k = ((bLon-aLon)*(lat-aLat) - (bLat-aLat)*(lon-aLon)) /
        (Math.pow(bLon-aLon, 2) + Math.pow(bLat-aLat, 2));
    LatLon projCoor = new LatLon(lat - k*(bLon-aLon), lon + k*(bLat-aLat));
    
    // If the projection isn't on the line segment a->b, set it to the closest out of a and b
    if (projCoor.lat() < Math.min(aLat, bLat) || projCoor.lat() > Math.max(aLat, bLat)) {
      if (point.greatCircleDistance(a.getCoor()) <= point.greatCircleDistance(b.getCoor())) {
        projCoor = a.getCoor();
      } else {
        projCoor = b.getCoor();
      }
    }
    
    return projCoor;
  }
  
  public boolean matchesPoint(LatLon point) {
    for (int i = 0; i < nodes.size()-1; ++i) {
      Node start = nodes.get(i);
      Node end = nodes.get(i+1);
      LatLon projection = projectPointBetweenPoints(point, start, end);
      double d = projection.greatCircleDistance(point);
      if (d < DISTANCE_THRESHOLD) {
        return true;
      }
    }
    return false;
  }
  
  public boolean matchesBearing(double bearing) {
    if (Math.abs(bearing - nodes.get(0).getCoor().bearing(nodes.get(1).getCoor()))
          < BEARING_THRESHOLD ||
        Math.abs(bearing - nodes.get(1).getCoor().bearing(nodes.get(0).getCoor()))
          < BEARING_THRESHOLD)
      return true;
    else
      return false;
  }
  
  public Way getWay() {
    return this.way;
  }
}
