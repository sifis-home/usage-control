/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package utility;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * This class contains all methods for reading / writing to Android Internal storage or asset storage.
 */
public final class AndroidFileUtility {

    private static final Logger log = Logger.getLogger( AndroidFileUtility.class.getName() );

    private AndroidFileUtility() {} // NOSONAR

    /**
     * Reads a file using the passed parameter as relative path from assets folder.
     * @param filePath
     *          a string that represents the absolute path to the file
     * @return the String that represents the content of the file
     */
    public static String readAssetFileAsString( String filePath, Context context ) {
        AssetManager assetManager = context.getAssets();
        StringBuilder out = new StringBuilder();
        try {
            InputStream inputStream = assetManager.open(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);   // add everything to StringBuilder
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

    /**
     * Read file as string starting from the app external storage (Android/data/package)
     * @param filePath relative path from external storage
     * @param context application context
     * @return file content
     */
    public static String readFileAsString( String filePath, Context context){
        File file = new File(context.getExternalFilesDir(null), filePath);

        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch( IOException exception ) {
            log.severe( "Unable to read file due to error: " + exception.getLocalizedMessage() );
            return null;
        }

        try (Scanner scanner = new Scanner( is )) {
            StringBuilder stringB = new StringBuilder();
            while( scanner.hasNext() ) {
                stringB.append( scanner.nextLine() );
            }
            return stringB.toString();
        }
    }
    /**
     * Write to file from the app external storage (Android/data/package)
     * @param content
     * @param filename
     * @return true if write has been performed
     */
    public static boolean writeStringToFile(String content, String filename, Context context){
        File file = new File(context.getExternalFilesDir(null), filename);
        if(file.exists()){
            log.info("File " + filename + " exist (path: " + file.getAbsolutePath() + ")");
        } else {
            log.info("File " + filename + " creating");
            file.getParentFile().mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String stripExtension( String name ) {
        if( name.contains( "." ) ) {
            return name.substring( 0, name.lastIndexOf( '.' ) );
        }
        return name;
    }

}
