package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.util.GuiHelper;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;

public final class Utils {
  private Utils() {
    // private constructor to avoid instantiation
  }
  
  public static void displayError(String error) {
    GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
        tr(error), tr("Error"), JOptionPane.WARNING_MESSAGE));
  }
  
  public static String getMGRSCoord(double lat, double lon) {
    String cell = MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(lat),
        Angle.fromDegreesLongitude(lon), 4).toString();
    String[] components = cell.split(" ");
    String squareID = components[0];
    Integer easting = 2 * Integer.parseInt(components[1]) / 5;
    Integer northing = 2 * Integer.parseInt(components[2]) / 5;
    cell = squareID + " " + easting.toString() + " " + northing.toString();
    return cell;
  }
}
