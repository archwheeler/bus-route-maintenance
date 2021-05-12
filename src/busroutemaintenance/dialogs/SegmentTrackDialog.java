package busroutemaintenance.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.tools.GBC;

@SuppressWarnings("serial")
public class SegmentTrackDialog extends BasicDialog implements ChangeListener {
  
  private final JPanel panel = new JPanel(new GridBagLayout());
  private final JLabel choiceLabel = new JLabel("Route type:", JLabel.CENTER);
  private final JRadioButton linearButton = new JRadioButton("Linear");
  private final JRadioButton repeatingButton = new JRadioButton("Repeating");
  private final ButtonGroup choiceButtons = new ButtonGroup();
  private final SpinnerModel minTimeModel = new SpinnerNumberModel(30,                  //initial
                                                                    1,                  //min
                                                                    Integer.MAX_VALUE,  //max
                                                                    1);                 //step
  private final JLabel timeLabel = new JLabel("Route time estimate (minutes):", JLabel.CENTER);
  private final JSpinner minTimeSpinner = new JSpinner(minTimeModel);
  
  private boolean isLinear = false;
  private int minTime = 30;
  
  public SegmentTrackDialog(String layerName) {
    super(tr("Segment GPX track"));
    
    linearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        isLinear = true;
      }
    });
    
    repeatingButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        isLinear = false;
      }
    });
    
    repeatingButton.setSelected(true);
    choiceButtons.add(repeatingButton);
    choiceButtons.add(linearButton);
    
    panel.add(choiceLabel, GBC.eol().fill(GBC.HORIZONTAL));
    JPanel buttons = new JPanel();
    buttons.add(repeatingButton);
    buttons.add(linearButton, GBC.eol().fill(GBC.HORIZONTAL));
    panel.add(buttons, GBC.eol().fill(GBC.HORIZONTAL));
    
    minTimeSpinner.addChangeListener(this);
    panel.add(timeLabel, GBC.eol().fill(GBC.HORIZONTAL));
    panel.add(minTimeSpinner);
    
    setContent(panel);
    
    showDialog();
  }
  
  public boolean isLinear() {
    return isLinear;
  }
  
  public int getMinTime() {
    return minTime;
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    minTime = (Integer) minTimeModel.getValue();
    System.out.println(minTime);
  }

}