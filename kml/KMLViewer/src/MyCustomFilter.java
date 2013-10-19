import java.io.File;

import javax.swing.filechooser.FileFilter;


public class MyCustomFilter extends FileFilter {

	 @Override
     public boolean accept(File file) {
         // Allow only directories, or files with ".kml" extension
         return file.isDirectory() || file.getAbsolutePath().endsWith(".kml");
     }
     @Override
     public String getDescription() {
         // This description will be displayed in the dialog,
         // hard-coded = ugly, should be done via I18N
         return "Text documents (*.kml)";
     }
}
