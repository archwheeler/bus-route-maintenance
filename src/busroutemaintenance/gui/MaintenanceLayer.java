package busroutemaintenance.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

import busroutemaintenance.Maintenance;

public class MaintenanceLayer extends Layer {
  
  private List<Maintenance> maintenance;

  public MaintenanceLayer(List<Maintenance> maintenance) {
    super(tr("Maintenance layer"));
    this.maintenance = maintenance;
    MainApplication.getLayerManager().addLayer(this);
  }

  @Override
  public void paint(Graphics2D g, MapView mv, Bounds bounds) {
    for (Maintenance m : maintenance) {
      Node startNode = m.getStartNode();
      Point startPoint = mv.getPoint(startNode);
      g.setColor(Color.RED);
      g.drawRect(startPoint.x, startPoint.y, 10, 10);
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

}
