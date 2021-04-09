package busroutemaintenance;

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

import javax.swing.JOptionPane;

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
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class SegmentTracksAction extends JosmActiveLayerAction implements MouseListener {

  private static final double MARKER_RANGE = 1e-3;
  private static final double MIN_ROUTE_TIME = 1800.0;
  private static final double MEAN_THRESHOLD = 0.50;
  private static final double MAX_TIMESTEP = 900.0;
  
  private enum Mode {
    None, Start, End
  }
  private MapFrame map;
  private Mode mode = Mode.None;
  private boolean isLinear;
  private GpxData activeData;
  private List<LatLon> markers;

  public SegmentTracksAction() {
    super(tr("Segment GPX track"), "segment", tr("Segment GPX track"),
        Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
        tr("Segment GPX track")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    Layer activeLayer = getActiveLayer();
    if (activeLayer == null) {
      noActiveLayerError();
      return;
    }
    
    SegmentTracksDialog dlg = new SegmentTracksDialog(activeLayer.getName());
    
    // if "Ok" pressed
    if (dlg.getValue() == 1) {
      activeData = getActiveData(activeLayer);
      if (activeData.getTrackCount() != 1) {
        GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
            tr("Expected a single track"), tr("Error"), JOptionPane.WARNING_MESSAGE));
        return;
      }
      
      isLinear = dlg.isLinear();
      
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
          if (Math.abs(s.getTime() - waypoint.getTime()) <= MIN_ROUTE_TIME) {
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
    double meanLength = 0.0;
    for (WayPoint wpt : waypoints) {
      segment.add(wpt);
      if (splits.contains(wpt)) {
        IGpxTrackSegment s = new GpxTrackSegment(segment);
        segments.add(s);
        meanLength += s.length();
        segment = new ArrayList<WayPoint>();
      }
    }
    IGpxTrackSegment s = new GpxTrackSegment(segment);
    segments.add(s);
    meanLength += s.length();
    
    meanLength /= segments.size();
    
    activeData.beginUpdate();
    activeData.removeTrack(track);
    for (IGpxTrackSegment seg : segments) {
      if (Math.abs(seg.length()/meanLength - 1.0) <= MEAN_THRESHOLD) {
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
