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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;


@XmlAccessorType( XmlAccessType.FIELD )
@Root( name = "AttributeSelectorType" )
public class AttributeSelectorType
        extends ExpressionType {

    @Attribute( name = "Category", required = true )
    protected String category;
    @Attribute( name = "ContextSelectorId" )
    protected String contextSelectorId;
    @Attribute( name = "Path", required = true )
    protected String path;
    @Attribute( name = "DataType", required = true )
    protected String dataType;
    @Attribute( name = "MustBePresent", required = true )
    protected boolean mustBePresent;

    public String getCategory() {
        return category;
    }

    public void setCategory( String value ) {
        this.category = value;
    }

    public String getContextSelectorId() {
        return contextSelectorId;
    }

    public void setContextSelectorId( String value ) {
        this.contextSelectorId = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath( String value ) {
        this.path = value;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType( String value ) {
        this.dataType = value;
    }

    public boolean isMustBePresent() {
        return mustBePresent;
    }

    public void setMustBePresent( boolean value ) {
        this.mustBePresent = value;
    }

}
