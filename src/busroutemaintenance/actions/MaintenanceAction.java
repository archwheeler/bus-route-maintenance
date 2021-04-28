package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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

import busroutemaintenance.Maintenance;
import busroutemaintenance.Utils;
import busroutemaintenance.dialogs.MaintenanceDialog;

@SuppressWarnings("serial")
public class MaintenanceAction extends JosmActiveLayerAction {
  
  private static final String OVERPASS_SERVER = "https://lz4.overpass-api.de/api/";
  private static final String OVERPASS_RELATION_QUERY = "relation[type=route][route=bus]"
                                                      + "(%f,%f,%f,%f); (._;>;); out meta;";
  private static final String OVERPASS_WAY_QUERY = "way[highway][highway!=service]"
                                                 + "[highway!=footway][highway!=cycleway]"
                                                 + "[highway!=pedestrian][highway!=steps]"
                                                 + "(%f,%f,%f,%f); (._;>;); out meta;";
  
  private Layer activeLayer;
  private Relation trackRelation;
  private Relation osmRelation;

  public MaintenanceAction() {
    super(tr("Bus route maintenance"), "maintenance", tr("Bus route maintenance"),
        Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
        tr("Bus route maintenance")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }
  
  Bounds getBounds(IGpxTrack track) {
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
    return new Bounds(minLat, minLon, maxLat, maxLon);
  }
  
  DataSet getOsmData(Bounds bounds, String query) throws OsmTransferException {
    OverpassDownloadReader reader = new OverpassDownloadReader(bounds, OVERPASS_SERVER,
        String.format(query, bounds.getMinLat(), bounds.getMinLon(),
                             bounds.getMaxLat(), bounds.getMaxLon()));
    
    ProgressMonitor monitor = new PleaseWaitProgressMonitor();
    DataSet osmData = reader.parseOsm(monitor);
    monitor.finishTask();
    
    return osmData;
  }
  
  List<Way> getRelationWays(Relation relation) {
    List<Way> ways = new ArrayList<Way>();
    for (RelationMember m : relation.getMembers()) {
      OsmPrimitiveType type = m.getType(); 
      if (type == OsmPrimitiveType.WAY) {
        ways.add(m.getWay());
      }
    }
    return ways;
  }
  
  double relationSimilarity(Relation a, Relation b) {
    Set<Way> setA = new HashSet<Way>(getRelationWays(a));
    Set<Way> setB = new HashSet<Way>(getRelationWays(b));
    
    Set<Way> intersection = new HashSet<Way>(setA);
    intersection.retainAll(setB);
    
    Set<Way> union = new HashSet<Way>(setA);
    union.addAll(setB);
    
    return (double) intersection.size() / (double) union.size();
  }
  
  Relation findClosestRelation(Relation relation, Bounds bounds) throws OsmTransferException {
    DataSet osmData = getOsmData(bounds, OVERPASS_RELATION_QUERY);
    double maxSimilarity = Double.NEGATIVE_INFINITY;
    Relation closestRelation = null;
    for (Relation r : osmData.getRelations()) {
      double similarity = relationSimilarity(relation, r);
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
        closestRelation = r;
      }
      System.out.println(String.format("%.2f", similarity));
    }
    return closestRelation;
  }
  
  Relation trackToRelation(IGpxTrack track, Bounds bounds) throws OsmTransferException {
    DataSet osmData = getOsmData(bounds, OVERPASS_WAY_QUERY);
    Collection<Way> allWays = osmData.getWays();
    List<Way> relationWays = new ArrayList<Way>();
    Way previousWay = null;
    LatLon prevCoor = null;
    LatLon coor = null;
    for (IGpxTrackSegment s : track.getSegments()) {
      for (WayPoint w : s.getWayPoints()) {
        prevCoor = coor;
        coor = w.getCoor();
        double minDistance = Double.POSITIVE_INFINITY;
        Way closestWay = null;
        for (Way way : allWays) {
          for (Node n : way.getNodes()) {
            double distance = coor.distance(n.getCoor());
            if (distance < minDistance) {
              minDistance = distance;
              closestWay = way;
            }
          }
        }
        if (!closestWay.equals(previousWay)) {
          LatLon firstCoor = closestWay.getNode(0).getCoor();
          LatLon secondCoor = closestWay.getNode(1).getCoor();
          if (prevCoor == null ||
              Math.abs(prevCoor.bearing(coor) - firstCoor.bearing(secondCoor)) < Math.PI / 4.0 ||
              Math.abs(prevCoor.bearing(coor) - secondCoor.bearing(firstCoor)) < Math.PI / 4.0) {
            relationWays.add(closestWay);
            previousWay = closestWay;
          }
        }
      }
    }
    
    Relation relation = new Relation();
    for (Way w : relationWays) {
      relation.addMember(new RelationMember(null, w));
    }
    osmData.addPrimitive(relation);
    return relation;
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
  
  List<Maintenance> computeMaintenance() {
    List<Maintenance> maintenance = new ArrayList<Maintenance>();

    return maintenance;
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
      
      IGpxTrack track = activeData.getTracks().iterator().next();
      Bounds bounds = getBounds(track);
      try {
        trackRelation = trackToRelation(track, bounds);
        displayRelation(trackRelation);
      } catch (OsmTransferException e) {
        Utils.displayError(e.getMessage());
        return;
      }
      try {
        osmRelation = findClosestRelation(trackRelation, bounds);
        displayRelation(osmRelation);
      } catch (OsmTransferException e) {
        Utils.displayError(e.getMessage());
        return;
      }
      
    }
    
    return;
  }

}
