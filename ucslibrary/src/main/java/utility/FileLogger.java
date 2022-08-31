package utility;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class responsible for performing logging into Android shared storage (accessible by different apps)
 * Can be useful for calculating timing overhead due to UCS introduction
 */
public class FileLogger {
    private final static String LOG_FILENAME = "experiment_try_access.log";
    private static Logger log = Logger.getLogger(FileLogger.class.getName());

    public static void appendLog(String text)
    {
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File logFile = new File(folder, LOG_FILENAME);
        if (!logFile.exists())
        {
            try
            {
                log.info("Creating new file");
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            log.info("Writing into file");
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
