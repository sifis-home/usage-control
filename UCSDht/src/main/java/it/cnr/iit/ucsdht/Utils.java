package it.cnr.iit.ucsdht;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class Utils {

    public static boolean createDir(File dir) {
        if (dir.exists()) {
            Utils.deleteDir(dir);
        }
        return dir.mkdir();
    }

    public static boolean deleteDir(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDir(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static String getResourcePath(Class<?> clazz) {

        URL input = clazz.getProtectionDomain().getCodeSource().getLocation();

        try {
            File myfile = new File(input.toURI());
            File dir = myfile.getParentFile(); // strip off .jar file
            return dir.getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readContent(InputStream input) {
        return new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    public static InputStream accessFile(Class<?> clazz, String fileName) {

        // this is the path within the jar file
        InputStream input = clazz.getResourceAsStream(
                File.separator + "resources" + File.separator + fileName);
        if (input == null) {
            // this is how we load file within editor
            input = clazz.getClassLoader().getResourceAsStream(fileName);
        }

        return input;
    }
}