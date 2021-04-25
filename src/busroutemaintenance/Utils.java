package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.util.GuiHelper;

public final class Utils {
  private Utils() {
    // private constructor to avoid instantiation
  }
  
  public static void displayError(String error) {
    GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(null,
        tr(error), tr("Error"), JOptionPane.WARNING_MESSAGE));
  }
}
