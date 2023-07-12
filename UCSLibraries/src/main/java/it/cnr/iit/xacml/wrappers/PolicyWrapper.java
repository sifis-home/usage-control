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
package it.cnr.iit.xacml.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import it.cnr.iit.ucs.exceptions.PolicyException;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.pap.PAPInterface;
import it.cnr.iit.utility.JAXBUtility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import it.cnr.iit.xacml.PolicyTags;

import oasis.names.tc.xacml.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml.core.schema.wd_17.RuleType;

/**
 * This is a wrapper for the policy class.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public class PolicyWrapper implements PolicyWrapperInterface {

    private static final Logger log = Logger.getLogger( PolicyWrapper.class.getName() );

    private static final int MAX_CONDITION_LENGTH = 20;

    private PolicyType policyType;
    private String policy;

    private PolicyWrapper() {}

    public static PolicyWrapper build( String policy ) throws PolicyException {
        PolicyWrapper policyWrapper = new PolicyWrapper();
        try {
            PolicyType policyType = unmarshalPolicyType( policy );
            policyWrapper.setPolicyType( policyType );
        } catch( JAXBException e ) {
            throw new PolicyException( "Error unmarshalling policy : {0}" + e.getMessage() );
        }
        policyWrapper.setPolicy( policy );
        return policyWrapper;
    }

    public static PolicyWrapper build( PolicyType policyType ) throws PolicyException {
        PolicyWrapper policyWrapper = new PolicyWrapper();
        try {
            String policy = marshalPolicyType( policyType );
            policyWrapper.setPolicy( policy );
        } catch( JAXBException e ) {
            throw new PolicyException( "Error marshalling policy : {0}" + e.getMessage() );
        }
        policyWrapper.setPolicyType( policyType );
        return policyWrapper;
    }

    public static PolicyWrapper build( PAPInterface pap, TryAccessMessage message ) throws PolicyException {
        String policy = message.getPolicy();
        if( (policy == null || policy.isEmpty()) && message.getPolicyId() != null && !message.getPolicyId().isEmpty() ) {
            policy = pap.retrievePolicy( message.getPolicyId() );
        }
        return PolicyWrapper.build( policy );
    }

    @Override
    public List<Attribute> getAttributesForCondition( String conditionName ) {
        Reject.ifBlank( conditionName );
        Reject.ifTrue( conditionName.length() > MAX_CONDITION_LENGTH );
        for( RuleType ruleType : policyType.getRuleTypeList() ) {
            List<ConditionType> conditionTypeList = ruleType.getCondition();
            if( conditionTypeList != null ) {
                for( ConditionType conditionType : conditionTypeList ) {
                    List<Attribute> attributeList = getAttributesFromCondition( conditionType, conditionName );
                    if( !attributeList.isEmpty() ) {
                        return attributeList;
                    }
                }
            }
        }
        log.log( Level.WARNING, "Condition not found : {0}", conditionName );
        return new ArrayList<>();
    }

    private List<Attribute> getAttributesFromCondition( ConditionType conditionType, String conditionName ) {
        if( conditionType.getDecisionTime() == null ) {
            if( conditionName.equals( PolicyTags.CONDITION_PRE ) ) {
                return getAttributesFromCondition( conditionType );
            }
        } else if( conditionType.getDecisionTime().equals( conditionName ) ) {
            return getAttributesFromCondition( conditionType );
        }
        return new ArrayList<>();
    }


    /**
     * Given a root node, build a number of lists equal to the number of attributes found within the tree
     * and save them in the elementList.
     * Each list should contain exactly one element whose value is of type AttributeDesignatorType, and one
     * or more elements whose value is of type AttributeValueType.
     * When this list (auxList) is fully populated, it is cloned and added to the elementList. Then, it is
     * reset to be used in the next recursive iterations.
     * @param node the root node we want start the iterations from
     * @param elementList list containing lists. Each list is related to one attribute and should contain exactly
     *                    one element whose value is of type AttributeDesignatorType, and one or more elements
     *                    whose value is of type AttributeValueType.
     * @param auxList a list that temporarily contains the information related to one attribute. When fully
     *                populated, this list is cloned and added to the elementList. Then, it is reset to be used
     *                in the next recursive iterations.
     */
    private void recursiveGetChildren(JAXBElement<?> node, List<ArrayList<JAXBElement<?>>> elementList, ArrayList<JAXBElement<?>> auxList) {
        Object objValue = node.getValue();
        if( objValue instanceof ApplyType ) {
            ApplyType applyType = (ApplyType) objValue;
            ArrayList<JAXBElement<?>> children = (ArrayList<JAXBElement<?>>) applyType.getExpression();
            boolean hasAnAttributeValueTypeChild = false;
            for (JAXBElement<?> jaxbElement : children) {
                Object child = jaxbElement.getValue();
                if (child instanceof AttributeValueType) {
                    // At least one direct child of this node is of type AttributeValueType
                    hasAnAttributeValueTypeChild = true;
                }
            }
            for (JAXBElement<?> jaxbElement : children) {
                recursiveGetChildren(jaxbElement, elementList, auxList);
            }
            if (hasAnAttributeValueTypeChild) {
                // if we get here it means that the node is of type ApplyType, it has at least
                // one child of type AttributeValueType, and the recursion of this node is terminated,
                // i.e., all its child nodes have been visited and the attributeList has been fully
                // populated.
                elementList.add((ArrayList<JAXBElement<?>>) auxList.clone());
                auxList.clear();
            }
        } else if (objValue instanceof AttributeValueType) {
            auxList.add(node);
        } else if (objValue instanceof AttributeDesignatorType) {
            auxList.add(node);
        }
    }


    /**
     * Function that effectively extracts the attributes from the condition.
     * The attribute object we have built up, embeds two different complex types
     * in the xsd: one is the AttributeDesignator, the other is the attribute
     * value.
     *
     * @param conditionType the condition we are analysing
     * @return the list of attributes contained in this condition.
     */
    private List<Attribute> getAttributesFromCondition( ConditionType conditionType ) {
        List<ArrayList<JAXBElement<?>>> elementList = new ArrayList<>();

        // populate the elementList as a list of lists. Each list contains
        // one or more elements whose value is of type AttributeValueType
        // and only one element whose value is of type AttributeDesignatorType
        recursiveGetChildren(conditionType.getExpression(), elementList, new ArrayList<>());

        ArrayList<Attribute> attributesList = new ArrayList<>();

        for (ArrayList<JAXBElement<?>> attributeValuesAndDesignatorList : elementList) {
            List<List<Object>> dataTypeAndValuesList = new ArrayList<>();

            Attribute attribute = new Attribute();

            // get the attribute values from the elements whose value is of type AttributeValueType
            // and store them in the dataTypeAndValuesList
            for (JAXBElement<?> jaxbElement : attributeValuesAndDesignatorList) {
                if (jaxbElement.getValue() instanceof AttributeValueType) {
                    AttributeValueType attributeValueType = (AttributeValueType) jaxbElement.getValue();
                    for (Object obj : attributeValueType.getContent()) {
                        dataTypeAndValuesList.add(Arrays.asList(attributeValueType.getDataType(), obj));
                    }
                }
            }

            // get the other info from the element whose value is of type AttributeDesignatorType,
            // build the attribute of type Attribute, and add it to the list that will be returned
            for (JAXBElement<?> jaxbElement : attributeValuesAndDesignatorList) {
                if (jaxbElement.getValue() instanceof AttributeDesignatorType) {
                    AttributeDesignatorType attrDesignatorType = (AttributeDesignatorType) jaxbElement.getValue();

                    attribute.setAttributeId(attrDesignatorType.getAttributeId());
                    attribute.setCategory(Category.toCATEGORY(attrDesignatorType.getCategory()));
                    attribute.setDataType(DataType.toDATATYPE(attrDesignatorType.getDataType()));

                    for (List<Object> o : dataTypeAndValuesList) {
                        // the first element of the list is the datatype, while the second is the attribute value
                        attribute.setAttributeValues(o.get(0).toString(), o.get(1).toString());
                    }
                    attributesList.add(attribute);
                }
            }
        }
        return attributesList;
    }


    @Override
    public String retrieveObligations() {
        log.log( Level.WARNING, "retrieveObligations is unimplemented" );
        return null;
    }

    @Override
    public String getRuleCombiningAlgorithmId() {
        return policyType.getRuleCombiningAlgId();
    }

    /**
     * In UXACML we may have 3 types of conditions: pre, ongoing and post.
     * This function retrieves the policy with the required condition.
     *
     * @param conditionName
     *          the required condition
     * @return a copy of the policyType containing only the required condition
     * @throws PolicyException
     */
    @Override
    public PolicyWrapper getPolicyForCondition( String conditionName ) throws PolicyException {
        PolicyType clonedPolicyType = clonePolicyTypeWithoutRules();
        List<Object> objectList = policyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();
        List<Object> clonedObjectList = clonedPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();

        for( Object obj : objectList ) {
            RuleType ruleType = (RuleType) obj;
            /* If this list of objects contains a ruleType with a condition list it must be analysed.
              In any other case the object will be copied inside cloned list. */
            if( !( obj instanceof RuleType ) ||
                    ( ruleType.getCondition() == null || ruleType.getCondition().isEmpty() ) ) {
                clonedObjectList.add( obj );
                continue;
            }
            analyseRuleType( clonedObjectList, ruleType, conditionName );
        }

        return PolicyWrapper.build( clonedPolicyType );
    }

    private void analyseRuleType( List<Object> objectList, RuleType ruleType, String conditionName ) {
        for( ConditionType conditionType : ruleType.getCondition() ) {
            RuleType clonedRuleType;
            if( conditionType.getDecisionTime() == null ) {
                if( conditionName.equals( PolicyTags.CONDITION_PRE ) ) {
                    clonedRuleType = cloneRuleType( ruleType, conditionType );
                } else {
                    clonedRuleType = getDefaultRuleType( "def-permit", EffectType.PERMIT );
                    clonedRuleType.setObligationExpressions( ruleType.getObligationExpressions() );
                }
                objectList.add( clonedRuleType );
            } else if( conditionType.getDecisionTime().equals( conditionName ) ) {
                clonedRuleType = cloneRuleType( ruleType, conditionType );
                objectList.add( clonedRuleType );
                break;
            }
        }
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy( String policy ) {
        this.policy = policy;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType( PolicyType policyType ) {
        this.policyType = policyType;
    }

    private RuleType getDefaultRuleType( String id, EffectType effectType ) {
        RuleType ruleType = new RuleType();
        ruleType.setEffect( effectType );
        ruleType.setRuleId( id );
        return ruleType;
    }

    /**
     * Performs a copy of the ruleType object.
     *
     * @param ruleType
     *          the ruleType object we want to copy
     * @param conditionType
     *          the condition to be put inside the new ruleType object
     * @return the ruleType object built in this way
     */
    private RuleType cloneRuleType( RuleType ruleType, ConditionType conditionType ) {
        RuleType newRuleType = new RuleType();
        newRuleType.getCondition().add( conditionType );
        newRuleType.setAdviceExpressions( ruleType.getAdviceExpressions() );
        newRuleType.setDescription( ruleType.getDescription() );
        newRuleType.setObligationExpressions( ruleType.getObligationExpressions() );
        newRuleType.setEffect( ruleType.getEffect() );
        newRuleType.setRuleId( ruleType.getRuleId() );
        newRuleType.setTarget( ruleType.getTarget() );

        return newRuleType;
    }

    /**
     * Performs a partial copy of the policyType object.
     *
     * @return the PolicyType object that is the copy of the one stored in this
     *         object
     */
    private PolicyType clonePolicyTypeWithoutRules() {
        PolicyType newPolicyType = new PolicyType();
        newPolicyType.setDescription( policyType.getDescription() );
        newPolicyType.setPolicyId( policyType.getPolicyId() );
        newPolicyType.setPolicyIssuer( policyType.getPolicyIssuer() );
        newPolicyType.setAdviceExpressions( policyType.getAdviceExpressions() );
        newPolicyType.setMaxDelegationDepth( policyType.getMaxDelegationDepth() );
        newPolicyType.setPolicyDefaults( policyType.getPolicyDefaults() );
        newPolicyType.setRuleCombiningAlgId( policyType.getRuleCombiningAlgId() );
        newPolicyType.setTarget( policyType.getTarget() );
        newPolicyType.setVersion( policyType.getVersion() );
        newPolicyType.setObligationExpressions( policyType.getObligationExpressions() );
        return newPolicyType;
    }

    private static PolicyType unmarshalPolicyType( String policy ) throws JAXBException {
        return JAXBUtility.unmarshalToObject( PolicyType.class, policy );
    }

    private static String marshalPolicyType( PolicyType policy ) throws JAXBException {
        return JAXBUtility.marshalToString( PolicyType.class, policy, PolicyTags.POLICY, JAXBUtility.SCHEMA );
    }

}
