package busroutemaintenance;

import javax.swing.JMenu;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import busroutemaintenance.actions.AverageTracksAction;
import busroutemaintenance.actions.MaintenanceAction;
import busroutemaintenance.actions.SegmentTracksAction;

public class BusRouteMaintenancePlugin extends Plugin {
  
  public BusRouteMaintenancePlugin(PluginInformation info) {
    super(info);
    
    JMenu dataMenu = MainApplication.getMenu().dataMenu;
    MainMenu.add(dataMenu, new SegmentTracksAction());
    MainMenu.add(dataMenu, new AverageTracksAction());
    MainMenu.add(dataMenu, new MaintenanceAction());
    
    ExtensionFileFilter.addImporter(new SiriFileImporter());
  }
  
}
