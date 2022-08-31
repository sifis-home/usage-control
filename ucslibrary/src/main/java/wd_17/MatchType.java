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
import org.simpleframework.xml.Root;

@Root( name = "Match")
public class MatchType {

    @Element( name = "AttributeValue", required = true )
    protected AttributeValueType attributeValue;
    @Element( name = "AttributeDesignator", required = false )
    protected AttributeDesignatorType attributeDesignator;
    @Element( name = "AttributeSelector", required = false )
    protected AttributeSelectorType attributeSelector;
    @Attribute( name = "MatchId", required = true )
    protected String matchId;

    public AttributeValueType getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue( AttributeValueType value ) {
        this.attributeValue = value;
    }

    public AttributeDesignatorType getAttributeDesignator() {
        return attributeDesignator;
    }

    public void setAttributeDesignator( AttributeDesignatorType value ) {
        this.attributeDesignator = value;
    }

    public AttributeSelectorType getAttributeSelector() {
        return attributeSelector;
    }

    public void setAttributeSelector( AttributeSelectorType value ) {
        this.attributeSelector = value;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId( String value ) {
        this.matchId = value;
    }

}
