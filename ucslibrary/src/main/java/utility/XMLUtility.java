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

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.StringWriter;

/**
 * This is the class devoted to store the utility required to deal with the
 * XML.
 *
 */
public final class XMLUtility {

    private XMLUtility() {

    }

    public static final String SCHEMA = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";

    /**
     * Takes an object which skeleton has been provided by the xjc utility and
     * marshals it into a string that represents the xml
     *
     * @param clazz
     *          the class to which the object belongs
     * @param object
     *          the object itself
     * @param name
     *          the name of the class of the object
     * @param schema
     *          the schema to be used, it can be null
     * @return a String that represents the xml of the object
     */
    public static final <T> String marshalToString( Class<T> clazz, T object,
            String name, String schema ){
        Serializer serializer = new Persister();
        StringWriter stringWriter = new StringWriter();
        try {
            serializer.write(object, stringWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringWriter.getBuffer().toString();
    }

    /**
     * Takes a String that represents the content of an xml and converts it into
     * one of the objects provided by the xjc tool.
     *
     * @param clazz
     *          the class to which the object belongs
     * @param xmlString
     *          the xml in string format
     * @return the object built up after unmarshalling, null otherwise
     */
    public static final <T> T unmarshalToObject( Class<T> clazz, String xmlString ){
        Serializer serializer = new Persister();
        T element = null;
        try {
            element = serializer.read(clazz, xmlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return element;
    }

}
