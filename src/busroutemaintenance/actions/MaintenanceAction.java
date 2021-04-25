package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.Shortcut;

import busroutemaintenance.Utils;
import busroutemaintenance.dialogs.MaintenanceDialog;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;

@SuppressWarnings("serial")
public class MaintenanceAction extends JosmActiveLayerAction {
  
  private static final String OVERPASS_SERVER = "https://lz4.overpass-api.de/api/";
  private static final String OVERPASS_QUERY = "relation[type=route][route=bus](%f,%f,%f,%f); "
                                             + "(._;>;); out meta;";
  private Layer activeLayer;

  public MaintenanceAction() {
    super(tr("Bus route maintenance"), "maintenance", tr("Bus route maintenance"),
        Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
        tr("Bus route maintenance")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }
  
  Bounds boundTrack(IGpxTrack track) {
    double minLat = Double.POSITIVE_INFINITY;
    double minLon = Double.POSITIVE_INFINITY;
    double maxLat = Double.NEGATIVE_INFINITY;
    double maxLon = Double.NEGATIVE_INFINITY;
    for (IGpxTrackSegment s : track.getSegments()) {
      for (WayPoint w : s.getWayPoints()) {
        double lat = w.lat();
        if (lat < minLat)
          minLat = lat;
        if (lat > maxLat)
          maxLat = lat;
        
        double lon = w.lon();
        if (lon < minLon)
          minLon = lon;
        if (lon > maxLon)
          maxLon = lon;
      }
    }
    return new Bounds(minLat, minLon, maxLat, maxLon); // Add wiggle room?
  }
  
  DataSet getNearbyRoutes(IGpxTrack track) throws OsmTransferException {
    Bounds bounds = boundTrack(track);
    OverpassDownloadReader reader = new OverpassDownloadReader(bounds, OVERPASS_SERVER,
        String.format(OVERPASS_QUERY, bounds.getMinLat(), bounds.getMinLon(),
                                      bounds.getMaxLat(), bounds.getMaxLon()));
    
    ProgressMonitor monitor = new PleaseWaitProgressMonitor();
    DataSet osmData = reader.parseOsm(monitor);
    monitor.finishTask();
    
    return osmData;
  }
  
  List<String> pointsToCells(List<LatLon> points) {
    List<List<List<String>>> H = new ArrayList<List<List<String>>>(4000);
    for (int i = 0; i < 4000; ++i) {
      List<List<String>> list = new ArrayList<List<String>>(4000);
      for (int j = 0; j < 4000; ++j) {
        list.add(j, null);
      }
      H.add(i, list);
    }
    List<String> cells = new ArrayList<String>();
    for (LatLon p : points) {
      String cell = MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(p.lat()),
                                         Angle.fromDegreesLongitude(p.lon()), 4).toString();
      String[] components = cell.split(" ");
      String squareID = components[0];
      Integer easting = 2 * Integer.parseInt(components[1]) / 5;
      Integer northing = 2 * Integer.parseInt(components[2]) / 5;
      cell = squareID + " " + easting.toString() + " " + northing.toString();
      List<String> previouslySeen = H.get(easting)
                                     .get(northing);
      if (previouslySeen == null) {
        previouslySeen = new LinkedList<String>();
        previouslySeen.add(squareID);
        H.get(easting).add(northing, previouslySeen);
        cells.add(cell);
      } else if (!previouslySeen.contains(squareID)) {
        previouslySeen.add(squareID);
        cells.add(cell);
      }
    }
    return cells;
  }
  
  List<String> routeToCells(IGpxTrack track) {
    List<LatLon> points = new ArrayList<LatLon>();
    for (IGpxTrackSegment s : track.getSegments()) {
      for (WayPoint w : s.getWayPoints())
        points.add(w.getCoor());
    }
    return pointsToCells(points);
  }
  
  List<String> routeToCells(Relation track) {
    List<LatLon> points = new ArrayList<LatLon>();
    for (RelationMember w : track.getMembers()) {
      OsmPrimitiveType type = w.getType(); 
      if (type == OsmPrimitiveType.WAY) {
        for (Node n : w.getWay().getNodes()) {
          points.add(n.getCoor());
        }
      }
    }
    return pointsToCells(points);
  }
  
  double cellsSimilarity(List<String> a, List<String> b) {
    Set<String> setA = new HashSet<String>(a);
    Set<String> setB = new HashSet<String>(b);
    
    Set<String> intersection = new HashSet<String>(setA);
    intersection.retainAll(setB);
    
    Set<String> union = new HashSet<String>(setA);
    union.addAll(setB);
    
    return (double) intersection.size() / (double) union.size();
  }
  
  Relation findClosestRoute(IGpxTrack track) throws OsmTransferException {
    List<String> trackCells = routeToCells(track);
    DataSet osmData = getNearbyRoutes(track);
    double maxSimilarity = Double.NEGATIVE_INFINITY;
    Relation closest = null;
    for (Relation r : osmData.getRelations()) {
      List<String> relationCells = routeToCells(r);
      double similarity = cellsSimilarity(trackCells, relationCells);
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
        closest = r;
      }
      System.out.println(String.format("%.2f", similarity));
    }
    return closest;
  }
  
  void displayRelation(Relation relation) {
    DataSet data = relation.getDataSet();
    for (Relation r : data.getRelations()) {
      if (!r.equals(relation)) {
        data.removePrimitive(r);
      }
    }
    Set<Way> ways = new HashSet<Way>();
    Set<Node> nodes = new HashSet<Node>();
    for (RelationMember m : relation.getMembers()) {
      OsmPrimitiveType type = m.getType(); 
      if (type == OsmPrimitiveType.WAY) {
        Way w = m.getWay();
        ways.add(w);
        for (Node n : w.getNodes()) {
          nodes.add(n);
        }
      }
    }
    for (Way w : data.getWays()) {
      if (!ways.contains(w))
        data.removePrimitive(w);
    }
    for (Node n : data.getNodes()) {
      if (!nodes.contains(n))
        data.removePrimitive(n);
    }
    
    OsmDataLayer layer = new OsmDataLayer(data, String.format("%s OSM", activeLayer.getName()),
                                          null);
    layerManager.addLayer(layer);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    activeLayer = getActiveLayer();
    if (activeLayer == null) {
      noActiveLayerError();
      return;
    }
    
    MaintenanceDialog dlg = new MaintenanceDialog(activeLayer.getName());
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      GpxData activeData = getActiveData(activeLayer);
      if (activeData.getTrackCount() != 1) {
        Utils.displayError(tr("Expected a single track"));
        return;
      }
      
      IGpxTrack activeTrack = activeData.getTracks().iterator().next();
      Relation closestRoute;
      try {
        closestRoute = findClosestRoute(activeTrack);
      } catch (OsmTransferException e) {
        Utils.displayError(e.getMessage());
        return;
      }
      
      // TEST CODE:
      System.out.println("Active route:\n" + routeToCells(activeTrack));
      if (closestRoute != null) {
        System.out.println("Closest route:\n" + routeToCells(closestRoute));
        displayRelation(closestRoute);
      } else {
        System.out.println("No closest route found");
      }
    }
    
    return;
  }

}
