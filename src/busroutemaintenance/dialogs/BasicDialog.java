package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;

@SuppressWarnings("serial")
public class BasicDialog extends ExtendedDialog {

  public BasicDialog(String title) {
    super(MainApplication.getMainFrame(), title, new String[] {tr("Ok"), tr("Cancel")});
  }

}
