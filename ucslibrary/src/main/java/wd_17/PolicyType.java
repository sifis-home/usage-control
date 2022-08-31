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
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Root( name = "Policy")
@Namespace(reference = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17")
public class PolicyType {

    @Element( name = "Description", required = false )
    protected String description;
    @Element( name = "PolicyIssuer", required = false )
    protected PolicyIssuerType policyIssuer;
    @Element( name = "PolicyDefaults", required = false )
    protected DefaultsType policyDefaults;
    @Element( name = "Target", required = true )
    protected TargetType target;
    @ElementListUnion( {
        @ElementList(inline = true, name = "CombinerParameters",
            type = CombinerParametersType.class ),
        @ElementList(inline = true, name = "RuleCombinerParameters",
            type = RuleCombinerParametersType.class ),
        @ElementList(inline = true, name = "VariableDefinition",
            type = VariableDefinitionType.class ),
        @ElementList(inline = true, type = RuleType.class ) } )
    protected List<Object> combinerParametersOrRuleCombinerParametersOrVariableDefinition;
    @Element( name = "ObligationExpressions", required = false )
    protected ObligationExpressionsType obligationExpressions;
    @Element( name = "AdviceExpressions", required = false )
    protected AdviceExpressionsType adviceExpressions;
    @Attribute( name = "PolicyId", required = true )
    protected String policyId;
    @Attribute( name = "Version", required = true )
    protected String version;
    @Attribute( name = "RuleCombiningAlgId", required = true )
    protected String ruleCombiningAlgId;
    @Attribute( name = "MaxDelegationDepth" , required = false)
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

    public DefaultsType getPolicyDefaults() {
        return policyDefaults;
    }

    public void setPolicyDefaults( DefaultsType value ) {
        this.policyDefaults = value;
    }

    public TargetType getTarget() {
        return target;
    }

    public void setTarget( TargetType value ) {
        this.target = value;
    }

    /**
     * This accessory method returns a reference to the live list, not a snapshot.
     * Therefore any modification you make to the returned list will be present
     * inside the JAXB object.
     */
    public List<Object> getCombinerParametersOrRuleCombinerParametersOrVariableDefinition() {
        if( combinerParametersOrRuleCombinerParametersOrVariableDefinition == null ) {
            combinerParametersOrRuleCombinerParametersOrVariableDefinition = new ArrayList<>();
        }
        return this.combinerParametersOrRuleCombinerParametersOrVariableDefinition;
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

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId( String value ) {
        this.policyId = value;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion( String value ) {
        this.version = value;
    }

    public String getRuleCombiningAlgId() {
        return ruleCombiningAlgId;
    }

    public void setRuleCombiningAlgId( String value ) {
        this.ruleCombiningAlgId = value;
    }

    public BigInteger getMaxDelegationDepth() {
        return maxDelegationDepth;
    }

    public void setMaxDelegationDepth( BigInteger value ) {
        this.maxDelegationDepth = value;
    }

    public List<RuleType> getRuleTypeList() {
        return getObjectsOfTypeToList( RuleType.class );
    }

    public List<ObligationsType> getObligationsTypeList() {
        return getObjectsOfTypeToList( ObligationsType.class );
    }

    private <T> List<T> getObjectsOfTypeToList( Class<T> clazz ) {
        List<Object> list = this.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();
        return (List<T>) list.stream()
            .filter( o -> clazz.isInstance( o ) )
            .collect( Collectors.toList() );
    }

}
