package busroutemaintenance.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

import busroutemaintenance.Utils;
import busroutemaintenance.dialogs.PlaceMarkerDialog;
import busroutemaintenance.dialogs.SegmentTrackDialog;

@SuppressWarnings("serial")
public class SegmentTrackAction extends BasicAction implements MouseListener {

  private static final double MARKER_RANGE = 1e-3;
  private static final double MEDIAN_THRESHOLD = 0.10;
  private static final double MAX_TIMESTEP = 900.0;
  
  private enum Mode {
    None, Start, End
  }
  private MapFrame map;
  private Mode mode = Mode.None;
  private boolean isLinear;
  private int minTime;
  private GpxData activeData;
  private List<LatLon> markers;

  public SegmentTrackAction() {
    super(tr("Segment GPS trajectory"), "segment", tr("Segment GPS trajectory"),
        Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
        tr("Segment GPS trajectory")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    Layer activeLayer = getActiveLayer();
    if (activeLayer == null) {
      noActiveLayerError();
      return;
    }
    
    SegmentTrackDialog dlg = new SegmentTrackDialog(activeLayer.getName());
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      activeData = getActiveData(activeLayer);
      if (activeData.getTrackCount() != 1) {
        Utils.displayError(tr("Expected a single track"));
        return;
      }
      
      isLinear = dlg.isLinear();
      minTime = dlg.getMinTime();
      
      PlaceMarkerDialog startDlg = new PlaceMarkerDialog(tr("start"));
      if (startDlg.getValue() == 1) {
        mode = Mode.Start;
        map = MainApplication.getMap();
        map.mapView.addMouseListener(this);
        markers = new ArrayList<LatLon>();
      }
    }
    
    return;
  }
  
  private void segmentData() {
    Collection<WayPoint> splits = new HashSet<WayPoint>();
    IGpxTrack track = activeData.getTracks().iterator().next();
    Collection<WayPoint> waypoints = track.getSegments().iterator().next().getWayPoints();
    
    WayPoint prevWpt = null;
    for (WayPoint wpt : waypoints) {
      if (prevWpt != null && wpt.getTime() - prevWpt.getTime() > MAX_TIMESTEP) {
        splits.add(prevWpt);
      }
      prevWpt = wpt;
    }
    
    for (LatLon marker : markers) {
      List<WayPoint> nearMarker = new ArrayList<WayPoint>();
      for (WayPoint waypoint : waypoints) {
        if (marker.distance(waypoint.getCoor()) <= MARKER_RANGE)
          nearMarker.add(waypoint);
      }
      Collections.sort(nearMarker, new Comparator<WayPoint>() {
        public int compare(WayPoint w1, WayPoint w2) {
          double d1 = marker.distance(w1.getCoor());
          double d2 = marker.distance(w2.getCoor());
          
          if (d1 == d2)
            return 0;
          return d1 < d2 ? -1 : 1;
        }
      });
      for (WayPoint waypoint : nearMarker) {
        boolean validSplit = true;
        for (WayPoint s : splits) {
          if (Math.abs(s.getTime() - waypoint.getTime()) <= minTime) {
            validSplit = false;
            break;
          }
        }
        if (validSplit)
          splits.add(waypoint);
      }
    }
    
    List<IGpxTrackSegment> segments = new ArrayList<IGpxTrackSegment>();
    List<WayPoint> segment = new ArrayList<WayPoint>();
    for (WayPoint wpt : waypoints) {
      segment.add(wpt);
      if (splits.contains(wpt)) {
        IGpxTrackSegment s = new GpxTrackSegment(segment);
        segments.add(s);
        segment = new ArrayList<WayPoint>();
      }
    }
    IGpxTrackSegment s = new GpxTrackSegment(segment);
    segments.add(s);
    
    List<Double> lengths = new ArrayList<Double>();
    for (IGpxTrackSegment seg : segments) {
      lengths.add(seg.length());
    }
    Collections.sort(lengths);
    double medianLength = lengths.get(lengths.size() / 2);
    
    activeData.beginUpdate();
    activeData.removeTrack(track);
    for (IGpxTrackSegment seg : segments) {
      if (Math.abs(seg.length()/medianLength - 1.0) <= MEDIAN_THRESHOLD) {
        List<IGpxTrackSegment> singleSegment = new ArrayList<IGpxTrackSegment>();
        singleSegment.add(seg);
        activeData.addTrack(new GpxTrack(singleSegment, Collections.<String, Object>emptyMap()));
      }
    }
    activeData.endUpdate();
  }
  
  private LatLon coordsAtCursor(Point p) {
    return map.mapView.getLatLon(p.x, p.y);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1)
      return;
    
    switch (mode) {
      case None:
        return;
        
      case Start:
        mode = Mode.None;
        markers.add(coordsAtCursor(e.getPoint()));
        if (isLinear) {
          PlaceMarkerDialog endDlg = new PlaceMarkerDialog(tr("end"));
          if (endDlg.getValue() == 1) {
            mode = Mode.End;
          }
        } else {
          map.mapView.removeMouseListener(this);
          segmentData();
        }
        return;
        
      case End:
        mode = Mode.None;
        map.mapView.removeMouseListener(this);
        markers.add(coordsAtCursor(e.getPoint()));
        segmentData();
        return;
        
      default:
        return;
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    return;
  }

  @Override
  public void mouseExited(MouseEvent e) {
    return;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    return;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    return;
  }

}
