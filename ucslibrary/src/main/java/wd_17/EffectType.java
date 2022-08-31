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

public enum EffectType {

//    PERMIT("Permit" ),
//    DENY("Deny" ),
    Permit("Permit"),
    Deny("Deny");
    private final String value;

    EffectType( String v ) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EffectType fromValue(String v ) {
        for( EffectType c : EffectType.values() ) {
            if( c.value.equals( v ) ) {
                return c;
            }
        }
        throw new IllegalArgumentException( v );
    }

}
