package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Shortcut;

import busroutemaintenance.Utils;
import busroutemaintenance.dialogs.AverageTracksDialog;

@SuppressWarnings("serial")
public class AverageTracksAction extends BasicAction {
  
  private static Color AVERAGE_COLOUR = Color.green;
  private static int S = 0;
  private static int M = 1;
  private static int D = 2;
  private static double THRESHOLD = 1.0;
  private static double MEDIAN_THRESHOLD = 0.05;
  private static int KMEANS_ITERATIONS = 10;

  public AverageTracksAction() {
    super(tr("Average GPX tracks"), "average", tr("Average GPX tracks"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Average GPX tracks")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }
  
  private static LatLon[] kMeans(List<LatLon> data, int k, int iterations) {
    LatLon[] centres = new LatLon[k];
    Random generator = new Random();
    for (int i = 0; i < k; ++i) {
      boolean unique = false;
      while (!unique) {
        centres[i] = data.get(generator.nextInt(data.size()));
        unique = true;
        for (int j = 0; j != i; ++j) {
          if (centres[i].equals(centres[j])) {
            unique = false;
            break;
          }
        }
      }
    }
    
    for (int i = 0; i < iterations; ++i) {
      List<List<LatLon>> clusters = new ArrayList<List<LatLon>>();
      for (int c = 0; c < k; ++c)
        clusters.add(new ArrayList<LatLon>());
      for (LatLon l : data) {
        double minDistance = Double.MAX_VALUE;
        int cluster = -1;
        for (int c = 0; c < k; ++c) {
          if (l.distance(centres[c]) < minDistance) {
            cluster = c;
            minDistance = l.distance(centres[c]);
          }
        }
        clusters.get(cluster).add(l);
      }
      for (int c = 0; c < k; ++c)
        centres[c] = averageLatLon(clusters.get(c));
    }
    
    return centres;
  }
  
  private static LatLon collectionGet(Collection<WayPoint> collection, int index) {
    if (index >= collection.size() || index < 0)
      return null;
    Iterator<WayPoint> iterator = collection.iterator();
    WayPoint w = null;
    while (index >= 0) {
      w = iterator.next();
      --index;
    }
    return w.getCoor();
  }
  
  private static LatLon averageLatLon(List<LatLon> points) {
    int size = 0;
    double lat = 0.0;
    double lon = 0.0;
    boolean valid = false;
    for (LatLon l : points) {
      if (l == null)
        continue;
      valid = true;
      size += 1;
      lat += l.lat();
      lon += l.lon();
    }
    return valid ? new LatLon(lat/size, lon/size) : null;
  }
  
  private static List<LatLon> removeOutliers(List<LatLon> points) {
    if (points == null)
      return null;
    if (points.size() <= 2)
      return points;
    LatLon average = averageLatLon(points);
    List<Double> distancesToAverage = new ArrayList<Double>();
    for (LatLon p : points) {
      if (p != null)
        distancesToAverage.add(p.distance(average));
    }
    
    List<LatLon> newPoints = new ArrayList<LatLon>();
    if (distancesToAverage.size() > 0) {
      Collections.sort(distancesToAverage);
      double medianDistance = distancesToAverage.get(distancesToAverage.size()/2);
      for (LatLon p : points) {
        if (p != null && Math.abs(p.distance(average)/medianDistance - 1.0) <= MEDIAN_THRESHOLD) {
          newPoints.add(p);
        }
      }
    }
    return newPoints;
  }
  
  private static List<LatLon> getMidpoints(GpxData data) {
    List<LatLon> midpoints = new ArrayList<LatLon>();
    List<IGpxTrackSegment> segments = data.getTrackSegmentsStream().collect(Collectors.toList());
    for (IGpxTrackSegment segment : segments) {
      Collection<WayPoint> waypoints = segment.getWayPoints();
      double midLength = segment.length() / 2.0;
      if (waypoints.size() <= 2 || midLength == 0.0) {
        midpoints.add(null);
        continue;
      }
      double processedLength = 0.0;
      WayPoint previous = null;
      for (WayPoint next : waypoints) {
        if (previous != null)
          processedLength += 1e5 * previous.getCoor().distance(next.getCoor());
        if (processedLength >= midLength)
          break;
        previous = next;
      }
      midpoints.add(previous.getCoor());
    }
    return midpoints;
  }
  
  private static LatLon[] lineSegment(GpxData data) {
    LatLon[] SMD = new LatLon[3];
    List<IGpxTrackSegment> segments = data.getTrackSegmentsStream().collect(Collectors.toList());
    List<LatLon> endpoints = new ArrayList<LatLon>();
    for (IGpxTrackSegment segment : segments) {
      Collection<WayPoint> waypoints = segment.getWayPoints();
      endpoints.add(collectionGet(waypoints, 0));
      endpoints.add(collectionGet(waypoints, waypoints.size()-1));
    }
    
    LatLon[] centres = kMeans(endpoints, 2, KMEANS_ITERATIONS);
    SMD[S] = centres[0];
    SMD[M] = averageLatLon((getMidpoints(data)));
    SMD[D] = centres[1];
    
    return SMD;
  }
  
  // returns the cosine of angle XYZ
  private static double cosAngle(LatLon x, LatLon y, LatLon z) {
    double a = y.distance(x);
    double b = y.distance(z);
    double c = x.distance(z);
    return (a*a + b*b - c*c) / (2*a*b);
  }
  
  private static List<WayPoint> curveSegment(GpxData data, LatLon s, LatLon d) {
    List<IGpxTrackSegment> segments = data.getTrackSegmentsStream().collect(Collectors.toList());
    List<LatLon> midpoints = getMidpoints(data);
    boolean containsNull = false;
    for (LatLon p : midpoints) {
      if (p == null)
        containsNull = true;
    }
    LatLon m = averageLatLon(removeOutliers(midpoints));
    
    List<WayPoint> curveSegment = new ArrayList<WayPoint>();
    if (m == null) {
      return curveSegment;
    } else if (containsNull) {
      curveSegment.add(new WayPoint(m));
    } else if (cosAngle(s, d, m) < THRESHOLD ||
               cosAngle(d, s, m) < THRESHOLD) {
      GpxData data1 = new GpxData();
      GpxData data2 = new GpxData();
      for (int i = 0; i < segments.size(); ++i) {
        LatLon mid = midpoints.get(i);
        Collection<WayPoint> waypoints = segments.get(i).getWayPoints();
        List<WayPoint> segment1 = new ArrayList<WayPoint>();
        List<WayPoint> segment2 = new ArrayList<WayPoint>();
        boolean beforeMid = true;
        for (WayPoint w : waypoints) {
          if (w.getCoor().equals(mid)) {
            segment1.add(w);
            beforeMid = false;
          } else if (beforeMid) {
            segment1.add(w);
          } else {
            segment2.add(w);
          }
        }
        List<IGpxTrackSegment> segments1 = new ArrayList<IGpxTrackSegment>();
        segments1.add(new GpxTrackSegment(segment1));
        
        List<IGpxTrackSegment> segments2 = new ArrayList<IGpxTrackSegment>();
        segments2.add(new GpxTrackSegment(segment2));
        
        if (segment1.get(0).getCoor().distance(s) < segment1.get(0).getCoor().distance(d)) {
          data1.addTrack(new GpxTrack(segments1, Collections.<String, Object>emptyMap()));
          data2.addTrack(new GpxTrack(segments2, Collections.<String, Object>emptyMap()));
        } else {
          data1.addTrack(new GpxTrack(segments2, Collections.<String, Object>emptyMap()));
          data2.addTrack(new GpxTrack(segments1, Collections.<String, Object>emptyMap()));
        }
      }
      
      List<WayPoint> curveSegment1 = curveSegment(data1, s, m);
      List<WayPoint> curveSegment2 = curveSegment(data2, m, d);
      curveSegment.addAll(curveSegment1);
      curveSegment.addAll(curveSegment2);
    } else {
      curveSegment.add(new WayPoint(m));
    }

    return curveSegment;
  }
  
  public GpxData averageTracks(GpxData data) throws IllegalDataException {
    if (data == null)
      throw new IllegalDataException(tr("Layer contains no GPX data"));
    if (data.getTrackSegsCount() < 2)
      throw new IllegalDataException(tr("Not enough segments to compute average"));
    
    List<IGpxTrackSegment> newSegments = new ArrayList<IGpxTrackSegment>();
    for (IGpxTrackSegment segment : data.getTrackSegmentsStream().collect(Collectors.toList())) {
      List<WayPoint> waypoints = new ArrayList<WayPoint>(segment.getWayPoints());
      Utils.increaseWayPoints(waypoints, 2);
      newSegments.add(new GpxTrackSegment(waypoints));
    }
    
    data = new GpxData();
    for (IGpxTrackSegment segment : newSegments) {
      List<IGpxTrackSegment> segments = new ArrayList<IGpxTrackSegment>();
      segments.add(segment);
      data.addTrack(new GpxTrack(segments, Collections.<String, Object>emptyMap()));
    }
    
    List<WayPoint> averageSegment = new ArrayList<WayPoint>();
    LatLon[] SMD = lineSegment(data);
    if (SMD[M] == null) {
      averageSegment.add(new WayPoint(SMD[S]));
      averageSegment.add(new WayPoint(SMD[D]));
    } else if (cosAngle(SMD[S], SMD[D], SMD[M]) < THRESHOLD ||
               cosAngle(SMD[D], SMD[S], SMD[M]) < THRESHOLD) {
      List<WayPoint> unsortedSegment = curveSegment(data, SMD[S], SMD[D]);
      WayPoint last = new WayPoint(SMD[S]);
      averageSegment.add(last);
      while (unsortedSegment.size() > 0) {
        WayPoint closest = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (WayPoint w : unsortedSegment) {
          if (w.getCoor().distance(last.getCoor()) < minDistance) {
            closest = w;
            minDistance = w.getCoor().distance(last.getCoor());
          }
        }
        unsortedSegment.remove(closest);
        if (minDistance < 0.05) {
          averageSegment.add(closest);
          last = closest; 
        }
      }
      averageSegment.add(new WayPoint(SMD[D]));
    } else {
      averageSegment.add(new WayPoint(SMD[S]));
      averageSegment.add(new WayPoint(SMD[M]));
      averageSegment.add(new WayPoint(SMD[D]));
    }
    
    GpxData average = new GpxData();
    List<IGpxTrackSegment> averageSegments = new ArrayList<IGpxTrackSegment>();
    averageSegments.add(new GpxTrackSegment(averageSegment));
    average.addTrack(new GpxTrack(averageSegments, Collections.<String, Object>emptyMap()));
    return average;
  }
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    Layer activeLayer = getActiveLayer();
    if (activeLayer == null) {
      noActiveLayerError();
      return;
    }
    
    AverageTracksDialog dlg = new AverageTracksDialog(activeLayer.getName());
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      GpxData activeData = getActiveData(activeLayer);
      
      try {
        GpxLayer averageLayer = new GpxLayer(averageTracks(activeData),
            String.format("%s AVG", activeLayer.getName()));
        averageLayer.setColor(AVERAGE_COLOUR);
        layerManager.addLayer(averageLayer);
      } catch (IllegalDataException e) {
        Utils.displayError(e.getMessage());
      }
    }
    
    return;
  }

}
