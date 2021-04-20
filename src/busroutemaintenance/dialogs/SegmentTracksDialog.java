package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.openstreetmap.josm.tools.GBC;

@SuppressWarnings("serial")
public class SegmentTracksDialog extends BasicDialog {
  
  private final JPanel panel = new JPanel(new GridBagLayout());
  private final JLabel choiceLabel = new JLabel("Is the GPX track linear or cyclic?",
                                                JLabel.CENTER);
  private final JRadioButton linearButton = new JRadioButton("Linear");
  private final JRadioButton cyclicButton = new JRadioButton("Cyclic");
  private final ButtonGroup choiceButtons = new ButtonGroup();
  
  private boolean isLinear = true;
  
  public SegmentTracksDialog(String layerName) {
    super(tr("Segment GPX track"));
    
    panel.add(choiceLabel, GBC.eol().fill(GBC.HORIZONTAL));
    
    linearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        isLinear = true;
      }
    });
    
    cyclicButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        isLinear = false;
      }
    });
    
    linearButton.setSelected(true);
    choiceButtons.add(linearButton);
    choiceButtons.add(cyclicButton);
    
    JPanel buttons = new JPanel(new GridBagLayout());
    buttons.add(linearButton);
    buttons.add(cyclicButton, GBC.eol().fill(GBC.HORIZONTAL));
    panel.add(buttons);
    
    setContent(panel);
    
    showDialog();
  }
  
  public boolean isLinear() {
    return isLinear;
  }

}