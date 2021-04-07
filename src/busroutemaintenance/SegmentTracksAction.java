package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class SegmentTracksAction extends JosmActiveLayerAction {

  private static final double MAX_TIMESTEP = 900.0; // 15 minutes

  public SegmentTracksAction() {
    super(tr("Segment GPX tracks"), "segment", tr("Segment GPX tracks"),
        Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
        tr("Segment GPX tracks")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
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
      GpxData activeData = getActiveData(activeLayer);
      if (activeData.getTrackCount() != 1) {
        GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
            tr("Expected a single track"), tr("Error"), JOptionPane.WARNING_MESSAGE));
        return;
      }
      
      List<IGpxTrackSegment> segments = new ArrayList<IGpxTrackSegment>();
      List<WayPoint> segment = new ArrayList<WayPoint>();
      IGpxTrack track = activeData.getTracks().iterator().next();
      Collection<WayPoint> waypoints = track.getSegments().iterator().next().getWayPoints();
      double wptTime;
      double prevTime = Double.MAX_VALUE;
      for (WayPoint wpt : waypoints) {
        wptTime = wpt.getTime();
        if (wptTime - prevTime > MAX_TIMESTEP) {
          segments.add(new GpxTrackSegment(segment));
          segment = new ArrayList<WayPoint>();
        }
        segment.add(wpt);
        prevTime = wptTime;
      }
      segments.add(new GpxTrackSegment(segment));
      
      activeData.beginUpdate();
      activeData.removeTrack(track);
      activeData.addTrack(new GpxTrack(segments, Collections.<String, Object>emptyMap()));
      activeData.endUpdate();
    }
    
    return;
  }

}
