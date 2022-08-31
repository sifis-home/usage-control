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
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "Apply")
public class ApplyType
        extends ExpressionType {

    @Element(name = "AttributeDesignator", required = false)
    protected AttributeDesignatorType attributeDesignatorType;
    @Element(name = "AttributeValue", required = false)
    protected AttributeValueType attributeValueType;
    @Element( name = "Description", required = false)
    protected String description;
    @ElementList(inline = true, required = false)
    protected List<ApplyType> applyTypeList;
    @Attribute( name = "FunctionId", required = true )
    protected String functionId;

    public String getDescription() {
        return description;
    }

    public void setDescription( String value ) {
        this.description = value;
    }

    public List<ApplyType> getExpression() { // NOSONAR
        if( applyTypeList == null ) {
            applyTypeList = new ArrayList<>();
        }
        return this.applyTypeList;
    }

    String getFunctionId() {
        return functionId;
    }

    public void setFunctionId( String value ) {
        this.functionId = value;
    }
    public AttributeDesignatorType getAttributeDesignatorType() {
        return attributeDesignatorType;
    }

    public void setAttributeDesignatorType(AttributeDesignatorType attributeDesignatorType) {
        this.attributeDesignatorType = attributeDesignatorType;
    }

    public AttributeValueType getAttributeValueType() {
        return attributeValueType;
    }

    public void setAttributeValueType(AttributeValueType attributeValueType) {
        this.attributeValueType = attributeValueType;
    }

}
