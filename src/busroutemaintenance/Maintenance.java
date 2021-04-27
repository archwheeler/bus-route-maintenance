package busroutemaintenance;

import java.util.List;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

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
    return start.firstNode();
  }

  public void carryOut(Relation relation) {
    List<RelationMember> newMembers = relation.getMembers();
    int index = 0;
    while (!newMembers.get(index).refersTo(start))
      ++index;
    ++index;
    while (index < newMembers.size() && !newMembers.get(index).refersTo(end))
      newMembers.remove(index);
    for (Way w : addWays)
      newMembers.add(index, new RelationMember(null, w));
    
    Relation newRelation = new Relation(relation);
    newRelation.setMembers(newMembers);
    ChangeCommand change = new ChangeCommand(relation, newRelation);
    UndoRedoHandler.getInstance().add(change);
  }
  
}
