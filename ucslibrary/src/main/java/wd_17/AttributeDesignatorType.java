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

@Root( name = "AttributeDesignatorType" )
public class AttributeDesignatorType
        extends ExpressionType {

    @Attribute( name = "Category", required = true )
    protected String category;
    @Attribute( name = "AttributeId", required = true )
    protected String attributeId;
    @Attribute( name = "DataType", required = true )
    protected String dataType;
    @Attribute( name = "Issuer", required = false )
    protected String issuer;
    @Attribute( name = "MustBePresent", required = true )
    protected boolean mustBePresent;

    public String getCategory() {
        return category;
    }

    public void setCategory( String value ) {
        this.category = value;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId( String value ) {
        this.attributeId = value;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType( String value ) {
        this.dataType = value;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer( String value ) {
        this.issuer = value;
    }

    public boolean isMustBePresent() {
        return mustBePresent;
    }

    public void setMustBePresent( boolean value ) {
        this.mustBePresent = value;
    }

}
