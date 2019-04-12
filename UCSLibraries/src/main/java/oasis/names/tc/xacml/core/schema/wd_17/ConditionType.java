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
//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB)
// Reference Implementation, v2.2.8-b130911.1802
// Vedere <a
// href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello
// schema di origine.
// Generato il: 2017.04.24 alle 12:34:54 PM CEST
//

package oasis.names.tc.xacml.core.schema.wd_17;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Classe Java per ConditionType complex type.
 *
 * <p>
 * Il seguente frammento di schema specifica il contenuto previsto contenuto in
 * questa classe.
 *
 * <pre>
 * &lt;complexType name="ConditionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:xacml:3.0:core:schema:wd-17}Expression"/>
 *       &lt;/sequence>
 *       &lt;attribute name="DecisionTime" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "ConditionType", propOrder = { "expression" } )
public class ConditionType {

    @XmlElementRef( name = "Expression",
        namespace = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
        type = JAXBElement.class )
    protected JAXBElement<?> expression;
    @XmlAttribute( name = "DecisionTime", required = true )
    protected String decisionTime;

    /**
     * Recupera il valore della proprietà expression.
     * 
     * @return possible object is {@link JAXBElement
     *         }{@code <}{@link AttributeSelectorType }{@code >}
     *         {@link JAXBElement }{@code <}{@link AttributeDesignatorType
     *         }{@code >} {@link JAXBElement }{@code <}{@link FunctionType
     *         }{@code >} {@link JAXBElement
     *         }{@code <}{@link VariableReferenceType }{@code >}
     *         {@link JAXBElement }{@code <}{@link AttributeValueType }{@code >}
     *         {@link JAXBElement }{@code <}{@link ApplyType }{@code >}
     *         {@link JAXBElement }{@code <}{@link ExpressionType }{@code >}
     * 
     */
    public JAXBElement<?> getExpression() { // NOSONAR
        return expression;
    }

    /**
     * Imposta il valore della proprietà expression.
     * 
     * @param value
     *          allowed object is {@link JAXBElement
     *          }{@code <}{@link AttributeSelectorType }{@code >}
     *          {@link JAXBElement }{@code <}{@link AttributeDesignatorType
     *          }{@code >} {@link JAXBElement }{@code <}{@link FunctionType
     *          }{@code >} {@link JAXBElement
     *          }{@code <}{@link VariableReferenceType }{@code >}
     *          {@link JAXBElement }{@code <}{@link AttributeValueType }{@code >}
     *          {@link JAXBElement }{@code <}{@link ApplyType }{@code >}
     *          {@link JAXBElement }{@code <}{@link ExpressionType }{@code >}
     * 
     */
    public void setExpression( JAXBElement<?> value ) {
        this.expression = value;
    }

    /**
     * Recupera il valore della proprietà decisionTime.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getDecisionTime() {
        return decisionTime;
    }

    /**
     * Imposta il valore della proprietà decisionTime.
     * 
     * @param value
     *          allowed object is {@link String }
     * 
     */
    public void setDecisionTime( String value ) {
        if( value == null ) {
            return;
        }
        this.decisionTime = value;
    }

}
