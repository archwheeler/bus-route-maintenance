package busroutemaintenance;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class SiriFileReader {
  
  private static int SIGNATURE = 0x53495249;
  
  private File file;
  
  public SiriFileReader(File file) {
    this.file = file;
  }
  
  public static boolean isSiriFile(File file) {
    try (DataInputStream is = new DataInputStream(new FileInputStream(file))) {
      int signature = is.readInt();
      return signature == SIGNATURE;
    } catch (Exception e) {
      return false;
    }
  }
  
}
