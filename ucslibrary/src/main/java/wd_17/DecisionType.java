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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.simpleframework.xml.Root;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Root( name = "DecisionType" )
public enum DecisionType {

//    PERMIT("Permit" ),
//    DENY("Deny" ),
//    INDETERMINATE("Indeterminate" ),
//    NOT_APPLICABLE("NotApplicable" ),
    Permit("Permit" ),
    Deny("Deny" ),
    Indeterminate("Indeterminate" ),
    NotApplicable("NotApplicable" );;
    private final String value;

    private static Map<String, DecisionType> namesMap = new HashMap<>( 4 );

    static {
        namesMap.put( "Permit", Permit );
        namesMap.put( "Deny", Deny );
        namesMap.put( "Indeterminate", Indeterminate );
        namesMap.put( "NotApplicable", NotApplicable );
    }

    @JsonCreator
    public static DecisionType forValue(String value ) {
        for( Entry<String, DecisionType> entry : namesMap.entrySet() ) {
            if( entry.getKey().equalsIgnoreCase( value ) ) {
                return entry.getValue();
            }
        }
        return null; // or fail
    }

    @JsonValue
    public String toValue() {
        for( Entry<String, DecisionType> entry : namesMap.entrySet() ) {
            if( entry.getValue() == this ) {
                return entry.getKey();
            }
        }
        return null; // or fail
    }

    DecisionType( String v ) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DecisionType fromValue(String v ) {
        for( DecisionType c : DecisionType.values() ) {
            if( c.value.equals( v ) ) {
                return c;
            }
        }
        throw new IllegalArgumentException( v );
    }

}
