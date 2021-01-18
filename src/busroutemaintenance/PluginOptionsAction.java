package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class PluginOptionsAction extends JosmAction {

  public PluginOptionsAction() {
    super(tr("Bus Route Maintenance options"), "options", tr("Bus Route Maintenance options"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Bus Route Maintenance options")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
}
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    PluginOptionsDialog dlg = new PluginOptionsDialog();
    if (dlg.getValue() == 1) {
        System.out.println("Button pressed");
    }
  }

}
