package busroutemaintenance;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.io.IllegalDataException;

public interface AverageTracksAlgorithm {
  public GpxData averageTracks(GpxData data) throws IllegalDataException;
}
