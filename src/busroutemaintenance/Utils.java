package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.util.GuiHelper;

public final class Utils {
  private Utils() {
    // private constructor to avoid instantiation
  }
  
  public static void displayError(String error) {
    GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
        tr(error), tr("Error"), JOptionPane.WARNING_MESSAGE));
  }
  
  public static void increaseWayPoints(List<WayPoint> waypoints, int factor) {
    if (waypoints.size() < 2)
      return;
    while (factor-- > 0) {
      WayPoint previous = waypoints.get(0);
      for (int i = 1; i < waypoints.size(); ++i) {
        WayPoint next = waypoints.get(i);
        LatLon midCoor = new LatLon((previous.lat()+next.lat())/2.0, (previous.lon()+next.lon())/2.0);
        WayPoint midPoint = new WayPoint(midCoor);
        waypoints.add(i, midPoint);
        previous = next;
        ++i;
      }
    }
  }
}
