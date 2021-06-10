package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.tools.GBC;

@SuppressWarnings("serial")
public class AverageTracksDialog extends BasicDialog {

  private final JPanel panel = new JPanel(new GridBagLayout());
  private final JLabel choiceLabel = new JLabel("Averaging method:", JLabel.CENTER);
  private final JRadioButton basicButton = new JRadioButton("Basic");
  private final JRadioButton experimentalButton = new JRadioButton("Experimental");
  private final ButtonGroup choiceButtons = new ButtonGroup();
  
  private boolean isBasic = true;
  
  public AverageTracksDialog(String layerName) {
    super(tr("Average GPS trajectories"));
    
    basicButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        isBasic = true;
      }
    });
    
    experimentalButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        isBasic = false;
      }
    });
    
    basicButton.setSelected(true);
    choiceButtons.add(basicButton);
    choiceButtons.add(experimentalButton);
    
    panel.setPreferredSize(new Dimension(300, 60));
    panel.add(choiceLabel, GBC.eol().fill(GBC.HORIZONTAL));
    JPanel buttons = new JPanel();
    buttons.add(basicButton);
    buttons.add(experimentalButton, GBC.eol().fill(GBC.HORIZONTAL));
    panel.add(buttons, GBC.eol().fill(GBC.HORIZONTAL));
    
    setContent(panel);
    
    showDialog();
  }
  
  public boolean isBasic() {
    return isBasic;
  }

}
