package busroutemaintenance;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class ImportSiriDataAction extends JosmAction {

  public ImportSiriDataAction() {
    super(tr("Import SIRI data"), "import", tr("Import SIRI data"),
            Shortcut.registerShortcut("edit:importsiridata", tr("Edit: {0}", tr("Import SIRI data")),
            KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
            true);
}
  
  @Override
  public void actionPerformed(ActionEvent arg0) {
    ImportSiriDataDialog dlg = new ImportSiriDataDialog();
    if (dlg.getValue() == 1) {
        System.out.println("Button pressed");
    }
  }

}
