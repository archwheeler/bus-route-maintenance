package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class AverageTracksAction extends JosmAction {

  public AverageTracksAction() {
    super(tr("Average GPX tracks"), "average", tr("Average GPX tracks"),
            Shortcut.registerShortcut("edit:busroutemaintenanceoptions", tr("Edit: {0}",
            tr("Average GPX tracks")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
}
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    AverageTracksDialog dlg = new AverageTracksDialog();
    if (dlg.getValue() == 1) {
        System.out.println("Button pressed");
    }
  }

}
