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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;

@XmlAccessorType( XmlAccessType.FIELD )
@Root( name = "Result")
public class ResultType {

    @Element( name = "Decision", required = true )
    @XmlSchemaType( name = "string" )
    protected DecisionType decision;
    @Element( name = "Status", required = false)
    protected StatusType status;
    @Element( name = "Obligations", required = false )
    protected ObligationsType obligations;
    @Element( name = "AssociatedAdvice", required = false )
    protected AssociatedAdviceType associatedAdvice;
    @ElementList(inline = true, required = false )
    protected List<AttributesType> attributes;
    @Element( name = "PolicyIdentifierList", required = false )
    protected PolicyIdentifierListType policyIdentifierList;

    public DecisionType getDecision() {
        return decision;
    }

    public void setDecision( DecisionType value ) {
        this.decision = value;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus( StatusType value ) {
        this.status = value;
    }

    public ObligationsType getObligations() {
        return obligations;
    }

    public void setObligations( ObligationsType value ) {
        this.obligations = value;
    }

    public AssociatedAdviceType getAssociatedAdvice() {
        return associatedAdvice;
    }

    public void setAssociatedAdvice( AssociatedAdviceType value ) {
        this.associatedAdvice = value;
    }

    public List<AttributesType> getAttributes() {
        if( attributes == null ) {
            attributes = new ArrayList<>();
        }
        return this.attributes;
    }

    public PolicyIdentifierListType getPolicyIdentifierList() {
        return policyIdentifierList;
    }

    public void setPolicyIdentifierList( PolicyIdentifierListType value ) {
        this.policyIdentifierList = value;
    }

}
