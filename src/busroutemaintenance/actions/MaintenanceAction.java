package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.Shortcut;

import busroutemaintenance.Maintenance;
import busroutemaintenance.Match;
import busroutemaintenance.Utils;
import busroutemaintenance.dialogs.MaintenanceDialog;
import busroutemaintenance.gui.MaintenanceLayer;

@SuppressWarnings("serial")
public class MaintenanceAction extends BasicAction {
  
  private static final String OVERPASS_SERVER = "https://lz4.overpass-api.de/api/";
  private static final String OVERPASS_QUERY = "(relation[type=route][route=bus]"
                                               + "(%f,%f,%f,%f);"
                                               + "way[highway][highway!=service]"
                                               + "[highway!=footway][highway!=cycleway]"
                                               + "[highway!=pedestrian][highway!=steps]"
                                               + "[highway!=track][highway!=path]"
                                               + "(%f,%f,%f,%f);); (._;>;); out meta;";
  private static enum Step {MATCH, INSERT, DELETE};
  
  private Layer activeLayer;
  private List<Way> trackWays;
  private Relation osmRelation;
  private DataSet osmData;
  private Set<OsmPrimitive> usedPrimitives = new HashSet<OsmPrimitive>();

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
                             bounds.getMaxLat(), bounds.getMaxLon(),
                             bounds.getMinLat(), bounds.getMinLon(),
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
  
  double relationSimilarity(List<Way> a, Relation b) {
    Set<Way> setA = new HashSet<Way>(a);
    Set<Way> setB = new HashSet<Way>(getRelationWays(b));
    
    Set<Way> intersection = new HashSet<Way>(setA);
    intersection.retainAll(setB);
    
    Set<Way> union = new HashSet<Way>(setA);
    union.addAll(setB);
    
    return (double) intersection.size() / (double) union.size();
  }
  
  Relation findClosestRelation(List<Way> ways, Bounds bounds) {
    double maxSimilarity = Double.NEGATIVE_INFINITY;
    Relation closestRelation = null;
    for (Relation r : osmData.getRelations()) {
      double similarity = relationSimilarity(ways, r);
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
        closestRelation = r;
      }
    }
    if (maxSimilarity < 0.2)
      return null;
    for (RelationMember m : closestRelation.getMembers()) {
      usedPrimitives.add(m.getMember());
      OsmPrimitiveType type = m.getType(); 
      if (type == OsmPrimitiveType.WAY) {
        Way w = m.getWay();
        usedPrimitives.addAll(w.getNodes());
      }
    }
    usedPrimitives.add(closestRelation);
    return closestRelation;
  }
  
  Way popLongestMatchingWay(List<WayPoint> points, Collection<Way> ways) {
    if (points.size() < 2) {
      points.clear();
      return null;
    }
    double bearing = points.get(0).getCoor().bearing(points.get(1).getCoor());
    
    List<Match> matches = new ArrayList<Match>();
    for (Way w : ways)
      matches.add(new Match(w));
    
    List<Match> badMatches = null;
    LatLon coor = null;
    LatLon prevCoor = null;
    double distance = 0.0;
    int point = 0;
    while (point < points.size() && matches.size() > 1) {
      prevCoor = coor;
      coor = points.get(point).getCoor();
      if (prevCoor != null)
        distance += prevCoor.greatCircleDistance(coor);
      badMatches = new ArrayList<Match>();
      for (Match match : matches) {
        if (!match.matchesPoint(coor))
          badMatches.add(match);
      }
      matches.removeAll(badMatches);
      ++point;
    }
    while (point-- > 0)
      points.remove(0);
    if (matches.size() == 1) {
      return matches.get(0).getWay();
    } else {
      Way closestWay = null;
      double closestDistance = Double.POSITIVE_INFINITY;
      for (Match m : badMatches) {
        Way way = m.getWay();
        double d =  Math.abs(distance - way.getLength());
        if (m.matchesBearing(bearing) && d < closestDistance) {
          closestWay = way;
          closestDistance = d;
        }
      }
      return closestWay;
    }
  }
  
  List<Way> trackToWays(IGpxTrack track, Bounds bounds) {
    List<WayPoint> trackPoints = new LinkedList<WayPoint>();
    for (IGpxTrackSegment s : track.getSegments()) {
      for (WayPoint w : s.getWayPoints()) {
        trackPoints.add(w);
      }
    }
    
    Utils.increaseWayPoints(trackPoints, 1);
    Collection<Way> allWays = osmData.getWays();
    List<Way> relationWays = new ArrayList<Way>();
    Way prevWay = null;
    
    while (!trackPoints.isEmpty()) {
      Way nextWay = popLongestMatchingWay(trackPoints, allWays);
      if (nextWay != null && !nextWay.equals(prevWay)) {
        relationWays.add(nextWay);
        usedPrimitives.add(nextWay);
        usedPrimitives.addAll(nextWay.getNodes());
        prevWay = nextWay;
      }
    }
    
    return relationWays;
  }
  
  void removeUnusedPrimitives() {
    for (OsmPrimitive p : osmData.getPrimitives(x -> true)) {
      if (!usedPrimitives.contains(p))
        osmData.removePrimitive(p.getOsmPrimitiveId());
    }
  }
  
  List<Maintenance> computeMaintenance() {
    List<Way> trackList = trackWays;
    List<Way> osmList = getRelationWays(osmRelation);
    int[][] scores = new int[trackList.size()+1][osmList.size()+1];
    Step[][] steps = new Step[trackList.size()+1][osmList.size()+1];
    for (int track = 1; track < trackList.size()+1; ++track) {
      for (int osm = 1; osm < osmList.size()+1; ++osm) {
        int maxScore = Integer.MIN_VALUE;
        Step bestStep = null;
        if (trackList.get(track-1).equals(osmList.get(osm-1)) &&
            scores[track-1][osm-1] + 1 > maxScore) {
          maxScore = scores[track-1][osm-1] + 1;
          bestStep = Step.MATCH;
        }
        if (scores[track-1][osm] > maxScore) {
          maxScore = scores[track-1][osm];
          bestStep = Step.INSERT;
        }
        if (scores[track][osm-1] > maxScore) {
          maxScore = scores[track][osm-1];
          bestStep = Step.DELETE;
        }
        scores[track][osm] = maxScore;
        steps[track][osm] = bestStep;
      }
    }
    
    List<Way> alignTrack = new ArrayList<Way>();
    List<Way> alignOsm = new ArrayList<Way>();
    int track = trackList.size();
    int osm = osmList.size();
    while (track > 0 && osm > 0) {
      Step step = steps[track][osm];
      switch (step) {
      case MATCH:
        alignTrack.add(trackList.get(track-1));
        alignOsm.add(osmList.get(osm-1));
        --track;
        --osm;
        break;
      case DELETE:
        alignTrack.add(null);
        alignOsm.add(osmList.get(osm-1));
        --osm;
        break;
      case INSERT:
        alignTrack.add(trackList.get(track-1));
        alignOsm.add(null);
        --track;
        break;
      }
    }
    for (int i = track-1; i >= 0; --i)
      alignTrack.add(trackList.get(i));
    for (int i = 0; i < osm; ++i)
      alignTrack.add(null);
    Collections.reverse(alignTrack);
    
    for (int i = osm-1; i >= 0; --i)
      alignOsm.add(osmList.get(i));
    for (int i = 0; i < track; ++i)
      alignOsm.add(null);
    Collections.reverse(alignOsm);
    
    List<Maintenance> maintenance = new ArrayList<Maintenance>();
    List<Way> addList = new ArrayList<Way>();
    Way start = null;
    int gap = 0;
    boolean jointEnd = false;
    for (int index = 0; index < alignTrack.size(); ++index) {
      Way trackWay = alignTrack.get(index);
      Way osmWay = alignOsm.get(index);
      jointEnd = false;
      if (trackWay == null) {
        ++gap;
      } else if (trackWay.equals(osmWay)) {
        if (gap > 0) {
          maintenance.add(new Maintenance(start, trackWay, addList));
          addList = new ArrayList<Way>();
        }
        jointEnd = true;
        start = trackWay;
        gap = 0;
      } else {
        ++gap;
        addList.add(trackWay);
      }
    }
    if (!jointEnd)
      maintenance.add(new Maintenance(start, null, addList));
    
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
        osmData = getOsmData(bounds, OVERPASS_QUERY);
      } catch (OsmTransferException e) {
        Utils.displayError(e.getMessage());
        return;
      }
      trackWays = trackToWays(track, bounds);
      osmRelation = findClosestRelation(trackWays, bounds);
      removeUnusedPrimitives();
      OsmDataLayer layer = new OsmDataLayer(osmData, "OSM data", null);
      layerManager.addLayer(layer);
      
      if (osmRelation != null) {
        List<Maintenance> maintenance = computeMaintenance();
        Layer maintenanceLayer = new MaintenanceLayer(maintenance, osmRelation);
        MainLayerManager layerManager = MainApplication.getLayerManager();
        layerManager.addLayer(maintenanceLayer);
        layerManager.setActiveLayer(maintenanceLayer);
      }
    }
    
    return;
  }

}
