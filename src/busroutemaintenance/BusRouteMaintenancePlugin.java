package busroutemaintenance;

import javax.swing.JMenu;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class BusRouteMaintenancePlugin extends Plugin {
  
  public BusRouteMaintenancePlugin(PluginInformation info) {
    super(info);
    JMenu dataMenu = MainApplication.getMenu().dataMenu;
    MainMenu.add(dataMenu, new ImportSiriDataAction());
  }
  
}
