package busroutemaintenance.average;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.io.IllegalDataException;

public interface AverageAlgorithm {
  public GpxData averageTracks(GpxData data) throws IllegalDataException;
}
