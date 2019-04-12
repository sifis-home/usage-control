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
package it.cnr.iit.xacmlutilities.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import it.cnr.iit.utility.JAXBUtility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacmlutilities.Attribute;
import it.cnr.iit.xacmlutilities.Category;
import it.cnr.iit.xacmlutilities.DataType;

import oasis.names.tc.xacml.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml.core.schema.wd_17.RuleType;

/**
 * This is a wrapper for the policy class.
 *
 * <p>
 * Using JAXB we do not need to manually parse the XML file anymore. However we
 * still need to access in a smart and fast ways the various parts of the XML
 * file. That is why we will use this class of helper functions to perform this
 * task. <br>
 * The Classes generated by xjc provide all the methods to perform get and set,
 * we will exploit those methods providing higher level classes.
 *
 * <br>
 * The methods we will need to extract the informations from the xml are:
 * <ol>
 * <li>retrieve attributes needed to evaluate a policy</li>
 * <li>retrieve on going attributes</li>
 * <li>retrieve obligations</li>
 * </ol>
 *
 * This class needs to be employed because in this way we can abstract the
 * underlying implementation, this means that if, in the future, we do not want
 * to use JAXB anymore but other methods we will have just to change the
 * implementation of those methods.
 * </p>
 * <p>
 * This PolicyHelper implements the PolicyHelperInterface, since, for the moment
 * we will use only the Policy tag (hence no policy set is used for now) and
 * since we want to use the JAXB to parse the xml, this class will extend the
 * PolicyType class provided by the JAXB.
 *
 * </p>
 * <p>
 * This class will use only factory methods and not constructor. This because it
 * is possible that the policy used does not contain some informations and we do
 * not want to allow the UCS to deal with damaged files.
 * </p>
 *
 * @author antonio
 *
 */
public class PolicyHelper implements PolicyHelperInterface {

    private static final Logger log = Logger.getLogger( PolicyHelper.class.getName() );

    private static final String MSG_ERR_UNMASHAL_POLICY = "Error unmarshalling policy : {0}";
    private static final String MSG_ERR_MARSHAL_POLICY = "Error marshalling policy : {0}";
    private static final String MSG_WARN_COND_NOT_FOUND = "Condition not found : {0}";

    // the policy type object
    private PolicyType policyType;

    /**
     * private constructor to avoid possible instantiation of this class
     */
    private PolicyHelper() {

    }

    /**
     * Builds a PolicyHelper object starting from the xml description provided in
     * the string
     *
     * @param string
     *          the xml that describes the policy
     * @return a PolicyHelper object if everything goes ok, null otherwise
     */
    public static PolicyHelper buildPolicyHelper( String string ) {
        // BEGIN PARAMETER CHECKING
        if( string == null || string.isEmpty() ) {
            throw new NullPointerException(
                "PolicyHelper.buildPolicyHelper string parameter is null or empty" );
        }
        // END PARAMETER CHECKING

        PolicyHelper policyHelper = new PolicyHelper();
        policyHelper.policyType = unmarshalPolicyType( string );
        return policyHelper.policyType != null ? policyHelper : null;
    }

    @Override
    public List<Attribute> getAttributesForCondition( String conditionName ) {
        Reject.ifBlank( conditionName );

        List<Object> list = policyType
            .getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();

        for( Object obj : list ) {
            if( obj.getClass().toString().contains( "RuleType" ) ) {
                RuleType ruleType = (RuleType) obj;
                List<ConditionType> conditions = ruleType.getCondition();
                if( conditions != null ) {
                    for( ConditionType conditionType : conditions ) {
                        if( conditionType.getDecisionTime() == null ) {
                            if( conditionName.equals( "pre" ) ) {
                                return extractAttributes( conditionType );
                            } else {
                                return new ArrayList<>();
                            }
                        }
                        if( conditionType.getDecisionTime().equals( conditionName ) ) {
                            return extractAttributes( conditionType );
                        }
                    }
                }
            }
        }

        log.warning( String.format( MSG_WARN_COND_NOT_FOUND, conditionName ) );
        return new ArrayList<>();
    }

    /**
     * Function that effectively extracts the attributes from the condition.
     * <p>
     * The attribute object we have built up, embeds two different complex types
     * in the xsd: one is the AttributeDesignator, the other is the attribute
     * value.
     * </p>
     *
     * @param conditionType
     *          the condition we are analyzing
     * @return the list of attributes types interested by this condition.
     */
    private List<Attribute> extractAttributes( ConditionType conditionType ) {
        ArrayList<JAXBElement<?>> listE = new ArrayList<>();
        ArrayList<Attribute> attributes = new ArrayList<>();
        listE.add( conditionType.getExpression() );
        Attribute attribute = new Attribute();
        int j = 0;
        for( int i = 0; i < listE.size(); i++ ) {

            JAXBElement<?> element = listE.get( i );
            if( element.getValue().getClass().toString().contains( "ApplyType" ) ) {
                listE.addAll( ( (ApplyType) element.getValue() ).getExpression() );
            } else if( element.getValue().getClass().toString()
                .contains( "AttributeDesignator" ) ) {
                AttributeDesignatorType attributeDesignatorType = (AttributeDesignatorType) element
                    .getValue();

                attributes.get( j ).setCategory(
                    Category.toCATEGORY( attributeDesignatorType.getCategory() ) );
                attributes.get( j )
                    .createAttributeId( attributeDesignatorType.getAttributeId() );
                attributes.get( j ).setAttributeDataType(
                    DataType.toDATATYPE( attributeDesignatorType.getDataType() ) );
                j += 1;
            } else if( element.getValue().getClass().toString()
                .contains( "AttributeValue" ) ) {
                AttributeValueType ad = (AttributeValueType) element.getValue();
                for( Object obj : ad.getContent() ) {
                    attribute.createAttributeValues( ad.getDataType(), obj.toString() );
                }
                attributes.add( attribute );
                attribute = new Attribute();
            }
        }
        return attributes;
    }

    @Override
    public String retrieveObligations() {
        return null;
    }

    @Override
    public String getRuleCombiningAlgorithmId() {
        return policyType.getRuleCombiningAlgId();
    }

    /**
     * Retrieves the particular condition for the evaluation.
     * <p>
     * Basically in a UXACML policy we may have more than one condition for a
     * rule, in general we have 3 conditions: one for pre, one for ongoing and the
     * other for post. With this function we basically want to extract the only
     * condition in which we're interested in. However we also want the new policy
     * to resemble the old one as much as possible.
     *
     * <br>
     *
     * Conditions are held in the RuleType inside the
     * combinerParametersOrRuleCombinerParametersOrVariableDefinition of the
     * PolicyType.
     *
     * Once a RuleType object inside that list has been identified, we look for
     * the condition in which we are interested in if the ruletype contiains
     * conditions, otherwise we simply put the rule inside the new policy. In
     * general ruletypes that do not contain any Condition are the default ones.
     * </p>
     *
     * @param conditionName
     *          the name of the condition in which we're interested into
     * @return a copy of the actual policy containing only the conditions in which
     *         we're interested into in the String format
     */
    @Override
    public String getConditionForEvaluation( String conditionName ) {
        PolicyType tmp = copyPolicy();

        /**
         * This is the most delicate part of this function.
         * <p>
         * Basically if in the list of
         * combinerParametersOrRuleCombinerParametersOrVariableDefinition there is a
         * ruleType then it must be analyzed in order to retrieve only the part of
         * the condition in which we're interested into. Otherwise the object will
         * be copied inside the copy policy type.
         * </p>
         */
        List<Object> list = policyType
            .getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();
        for( Object obj : list ) {
            if( obj.getClass().toString().contains( "RuleType" ) ) {
                RuleType ruleType = (RuleType) obj;
                // check if the ruletype contians any conditions
                if( ruleType.getCondition() != null
                        && ruleType.getCondition().isEmpty() ) {
                    List<ConditionType> conditions = ruleType.getCondition();
                    for( ConditionType conditionType : conditions ) {

                        if( conditionType.getDecisionTime() == null ) {
                            if( conditionName.equals( "pre" ) ) {
                                RuleType tmpRuleType = copyRuleType( ruleType, conditionType );
                                tmp.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()
                                    .add( tmpRuleType );
                            } else {
                                RuleType tmpRuleType = DefaultPermitRule.getInstance();
                                tmpRuleType.setObligationExpressions(
                                    ruleType.getObligationExpressions() );
                                tmp.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()
                                    .add( tmpRuleType );
                            }
                        } else if( conditionType.getDecisionTime().equals( conditionName ) ) {
                            RuleType tmpRuleType = copyRuleType( ruleType, conditionType );
                            tmp.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()
                                .add( tmpRuleType );
                            break;
                        }
                    }
                } else {
                    tmp.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()
                        .add( ruleType );
                }
            } else {
                tmp.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()
                    .add( obj );
            }
        }

        return marshalPolicyType( tmp );
    }

    public static PolicyType unmarshalPolicyType( String policy ) {
        try {
            return JAXBUtility.unmarshalToObject( PolicyType.class, policy );
        } catch( Exception e ) {
            log.severe( String.format( MSG_ERR_UNMASHAL_POLICY, e.getMessage() ) );
        }
        return null;
    }

    public static String marshalPolicyType( PolicyType policy ) {
        try {
            return JAXBUtility.marshalToString( PolicyType.class, policy, "Policy",
                JAXBUtility.SCHEMA );
        } catch( JAXBException e ) {
            log.severe( String.format( MSG_ERR_MARSHAL_POLICY, e.getMessage() ) );
        }
        return null;
    }

    /**
     * Copies the ruletype passed as parameter building a new ruletype object.
     * Instead of copying all the conditions, however, it simply copies the one
     * that we pass as parameter.
     *
     * @param ruleType
     *          the ruletype object we want to copy
     * @param conditionType
     *          the condition to be put inside the new ruletype object
     * @return the ruletype object built in this way
     */
    private RuleType copyRuleType( RuleType ruleType,
            ConditionType conditionType ) {
        RuleType tmpRuleType = new RuleType();
        tmpRuleType.getCondition().add( conditionType );
        tmpRuleType.setAdviceExpressions( ruleType.getAdviceExpressions() );
        tmpRuleType.setDescription( ruleType.getDescription() );
        tmpRuleType.setObligationExpressions( ruleType.getObligationExpressions() );
        tmpRuleType.setEffect( ruleType.getEffect() );
        tmpRuleType.setRuleId( ruleType.getRuleId() );
        tmpRuleType.setTarget( ruleType.getTarget() );

        return tmpRuleType;
    }

    /**
     * Performs a copy of the policy object stored in this object.
     * <p>
     * The only part that will be left outside is the part is the list
     * combinerParametersOrRuleCombinerParametersOrVariableDefinition which will
     * be analyzed later. This because we may require in the new policy to have
     * only a part of that list
     * </p>
     *
     * @return the PolicyType object that is the copy of the one stored in this
     *         object
     */
    private PolicyType copyPolicy() {
        PolicyType tmpPolicyType = new PolicyType();
        tmpPolicyType.setDescription( policyType.getDescription() );
        tmpPolicyType.setPolicyId( policyType.getPolicyId() );
        tmpPolicyType.setPolicyIssuer( policyType.getPolicyIssuer() );
        tmpPolicyType.setAdviceExpressions( policyType.getAdviceExpressions() );
        tmpPolicyType.setMaxDelegationDepth( policyType.getMaxDelegationDepth() );
        tmpPolicyType.setPolicyDefaults( policyType.getPolicyDefaults() );
        tmpPolicyType.setRuleCombiningAlgId( policyType.getRuleCombiningAlgId() );
        tmpPolicyType.setTarget( policyType.getTarget() );
        tmpPolicyType.setVersion( policyType.getVersion() );
        tmpPolicyType.setObligationExpressions( policyType.getObligationExpressions() );
        return tmpPolicyType;
    }

}
