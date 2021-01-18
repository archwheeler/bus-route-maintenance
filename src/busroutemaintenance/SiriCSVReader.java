package busroutemaintenance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

public class SiriCSVReader {

  private static final String SEPARATOR = ",";
  private static final int NFIELDS = 9;

  private static String[] processCSVLine(String line) {
    if (line == null || line.length() == 0)
      return null;
    
    StringTokenizer st = new StringTokenizer(line, SEPARATOR, false);
    int nfields = st.countTokens();
    
    String[] fields = new String[nfields];
    for (int i = 0; i < nfields; i++)
      fields[i] = st.nextToken().trim();
    return fields;
  }
  
  public static boolean isSiriFile(File file) {
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String firstLine = br.readLine();
      return firstLine != null && processCSVLine(firstLine).length == NFIELDS;
    } catch (Exception e) {
      return false;
    }
  }
  
}
