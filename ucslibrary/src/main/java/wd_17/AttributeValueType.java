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
package wd_17;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.ArrayList;
import java.util.List;

@Root( name = "AttributeValue")
public class AttributeValueType {


    @Attribute( name = "DataType", required = true )
    protected String dataType;
//    @ElementMap(entry="property", key="key", attribute=true, inline=true, required = false)
//    private Map<QName, String> otherAttributes = new HashMap<>();
    @Text(required = false)
    private String content;

    public List<Object> getContent() {
        List<Object> contentObj = new ArrayList<>();
        if( content != null ) {
            contentObj.add(content);
        }
        return contentObj;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType( String value ) {
        this.dataType = value;
    }

    public void setContent(String content) {this.content = content; }
//    public Map<QName, String> getOtherAttributes() {
//        return otherAttributes;
//    }

}
