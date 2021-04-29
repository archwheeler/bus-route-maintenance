package busroutemaintenance;

import java.util.List;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class Maintenance {
  
  private Way start;
  private Way end;
  private  List<Way> addWays;
  
  public Maintenance(Way start, Way end, List<Way> addWays) {
    this.start = start;
    this.end = end;
    this.addWays = addWays;
  }
  
  public Node getStartNode() {
    if (start != null)
      return start.firstNode();
    else
      return end.firstNode();
  }

  public void carryOut(Relation osmRelation) {
    List<RelationMember> newMembers = osmRelation.getMembers();
    int index = 0;
    if (start != null) {
      while (!newMembers.get(index++).getMember().equals(start));
    }
    while (index < newMembers.size() && !newMembers.get(index).getMember().equals(end))
      newMembers.remove(index);
    DataSet osmData = osmRelation.getDataSet();
    osmData.beginUpdate();
    for (Way w : addWays) {
      RelationMember newMember = null;
      if (osmData.containsWay(w)) {
        newMember = new RelationMember(null, osmData.getPrimitiveById(w.getOsmPrimitiveId()));
      } else {
        for (Node n : w.getNodes())
          osmData.addPrimitive(n);
        osmData.addPrimitive(w);
        newMember = new RelationMember(null, w);
      }
      newMembers.add(index, newMember);
    }
    
    Relation newRelation = new Relation(osmRelation);
    newRelation.setMembers(newMembers);
    ChangeCommand change = new ChangeCommand(osmRelation, newRelation);
    UndoRedoHandler.getInstance().addNoRedraw(change);
    UndoRedoHandler.getInstance().afterAdd(change);
    osmData.endUpdate();
    
    List<OsmDataLayer> layers = MainApplication.getLayerManager()
                                               .getLayersOfType(OsmDataLayer.class);
    for (OsmDataLayer layer : layers) {
      if (layer.data.equals(osmData)) {
        GenericRelationEditor editor = (GenericRelationEditor) RelationEditor.getEditor(layer,
                                                                                        osmRelation,
                                                                                        newMembers);
        editor.setVisible(true);
        return;
      }
    }
  }
  
}
