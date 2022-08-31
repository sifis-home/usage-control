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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "AdviceType")
public class AdviceType {

    @ElementList( name = "AttributeAssignment", required = false)
    protected List<AttributeAssignmentType> attributeAssignment;
    @Attribute( name = "AdviceId", required = true )
    protected String adviceId;

    public List<AttributeAssignmentType> getAttributeAssignment() {
        if( attributeAssignment == null ) {
            attributeAssignment = new ArrayList<>();
        }
        return this.attributeAssignment;
    }

    public String getAdviceId() {
        return adviceId;
    }

    public void setAdviceId( String value ) {
        this.adviceId = value;
    }

}
