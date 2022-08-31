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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlSchemaType;

@XmlAccessorType( XmlAccessType.FIELD )
@Root( name = "PolicySetType")
public class PolicySetType {

    @Element( name = "Description" )
    protected String description;
    @Element( name = "PolicyIssuer" )
    protected PolicyIssuerType policyIssuer;
    @Element( name = "PolicySetDefaults" )
    protected DefaultsType policySetDefaults;
    @Element( name = "Target", required = true )
    protected TargetType target;
    @XmlElementRefs( {
        @XmlElementRef( name = "CombinerParameters", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", type = Serializable.class,
            required = false ),
        @XmlElementRef( name = "PolicySetIdReference", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
            type = Serializable.class, required = false ),
        @XmlElementRef( name = "Policy", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", type = Serializable.class,
            required = false ),
        @XmlElementRef( name = "PolicyCombinerParameters", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
            type = Serializable.class, required = false ),
        @XmlElementRef( name = "PolicySet", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", type = Serializable.class,
            required = false ),
        @XmlElementRef( name = "PolicyIdReference", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", type = Serializable.class,
            required = false ),
        @XmlElementRef( name = "PolicySetCombinerParameters", namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
            type = Serializable.class, required = false )
    } )
    protected List<Serializable> policySetOrPolicyOrPolicySetIdReference;
    @Element( name = "ObligationExpressions" )
    protected ObligationExpressionsType obligationExpressions;
    @Element( name = "AdviceExpressions" )
    protected AdviceExpressionsType adviceExpressions;
    @Attribute( name = "PolicySetId", required = true )
    @XmlSchemaType( name = "anyURI" )
    protected String policySetId;
    @Attribute( name = "Version", required = true )
    protected String version;
    @Attribute( name = "PolicyCombiningAlgId", required = true )
    @XmlSchemaType( name = "anyURI" )
    protected String policyCombiningAlgId;
    @Attribute( name = "MaxDelegationDepth" )
    protected BigInteger maxDelegationDepth;

    public String getDescription() {
        return description;
    }

    public void setDescription( String value ) {
        this.description = value;
    }

    public PolicyIssuerType getPolicyIssuer() {
        return policyIssuer;
    }

    public void setPolicyIssuer( PolicyIssuerType value ) {
        this.policyIssuer = value;
    }

    public DefaultsType getPolicySetDefaults() {
        return policySetDefaults;
    }

    public void setPolicySetDefaults( DefaultsType value ) {
        this.policySetDefaults = value;
    }

    public TargetType getTarget() {
        return target;
    }

    public void setTarget( TargetType value ) {
        this.target = value;
    }

    public List<Serializable> getPolicySetOrPolicyOrPolicySetIdReference() { // NOSONAR
        if( policySetOrPolicyOrPolicySetIdReference == null ) {
            policySetOrPolicyOrPolicySetIdReference = new ArrayList<>();
        }
        return this.policySetOrPolicyOrPolicySetIdReference;
    }

    public ObligationExpressionsType getObligationExpressions() {
        return obligationExpressions;
    }

    public void setObligationExpressions( ObligationExpressionsType value ) {
        this.obligationExpressions = value;
    }

    public AdviceExpressionsType getAdviceExpressions() {
        return adviceExpressions;
    }

    public void setAdviceExpressions( AdviceExpressionsType value ) {
        this.adviceExpressions = value;
    }

    public String getPolicySetId() {
        return policySetId;
    }

    public void setPolicySetId( String value ) {
        this.policySetId = value;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion( String value ) {
        this.version = value;
    }

    public String getPolicyCombiningAlgId() {
        return policyCombiningAlgId;
    }

    public void setPolicyCombiningAlgId( String value ) {
        this.policyCombiningAlgId = value;
    }

    public BigInteger getMaxDelegationDepth() {
        return maxDelegationDepth;
    }

    public void setMaxDelegationDepth( BigInteger value ) {
        this.maxDelegationDepth = value;
    }

}
