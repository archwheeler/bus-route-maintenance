package busroutemaintenance.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

import busroutemaintenance.Maintenance;

public class MaintenanceLayer extends Layer implements MouseListener {
  
  private static final int SQUARE_SIZE = 32;
  private static final double DISTANCE_THRESHOLD = 32;
  
  private List<Maintenance> maintenance;
  private Relation relation;
  private MapFrame map;

  public MaintenanceLayer(List<Maintenance> maintenance, Relation relation) {
    super(tr("Maintenance layer"));
    this.maintenance = maintenance;
    this.relation = relation;
    this.map = MainApplication.getMap();
    map.mapView.addMouseListener(this);
    MainApplication.getLayerManager().addLayer(this);
  }

  @Override
  public void paint(Graphics2D g, MapView mv, Bounds bounds) {
    for (Maintenance m : maintenance) {
      Node startNode = m.getStartNode();
      Point startPoint = mv.getPoint(startNode);
      g.setColor(Color.RED);
      g.fillRect(startPoint.x, startPoint.y, SQUARE_SIZE, SQUARE_SIZE);
    }
  }

  @Override
  public Icon getIcon() {
    return ImageProvider.get("layer", "osmdata_small");
  }

  @Override
  public Object getInfoComponent() {
    return getToolTipText();
  }

  @Override
  public Action[] getMenuEntries() {
    return new Action[] {LayerListDialog.getInstance().createShowHideLayerAction(),
        LayerListDialog.getInstance().createDeleteLayerAction(), SeparatorLayerAction.INSTANCE,
        new RenameLayerAction(null, this), SeparatorLayerAction.INSTANCE,
        new LayerListPopup.InfoAction(this) };
  }

  @Override
  public String getToolTipText() {
    return getName();
  }

  @Override
  public boolean isMergable(Layer arg0) {
    return false;
  }

  @Override
  public void mergeFrom(Layer arg0) {
    return;
  }

  @Override
  public void visitBoundingBox(BoundingXYVisitor arg0) {
    return;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1 ||
        !MainApplication.getLayerManager().getActiveLayer().equals(this))
      return;
    Point click = e.getPoint();
    for (Maintenance m : maintenance) {
      Point maintenancePoint = map.mapView.getPoint(m.getStartNode());
      if (click.distance(maintenancePoint) < DISTANCE_THRESHOLD) {
        m.carryOut(relation);
        maintenance.remove(m);
        if (maintenance.size() == 0)
          map.mapView.removeMouseListener(this);
        return;
      }
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    return;
  }

  @Override
  public void mouseExited(MouseEvent e) {
    return;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    return;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    return;
  }

}
