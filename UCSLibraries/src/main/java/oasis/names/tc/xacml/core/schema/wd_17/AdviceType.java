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
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine.
// Generato il: 2017.04.24 alle 12:34:54 PM CEST
//

package oasis.names.tc.xacml.core.schema.wd_17;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Classe Java per AdviceType complex type.
 *
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 *
 * <pre>
 * &lt;complexType name="AdviceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:xacml:3.0:core:schema:wd-17}AttributeAssignment" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="AdviceId" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "AdviceType", propOrder = {
    "attributeAssignment"
} )
public class AdviceType {

    @XmlElement( name = "AttributeAssignment" )
    protected List<AttributeAssignmentType> attributeAssignment;
    @XmlAttribute( name = "AdviceId", required = true )
    @XmlSchemaType( name = "anyURI" )
    protected String adviceId;

    /**
     * Gets the value of the attributeAssignment property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attributeAssignment property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttributeAssignment().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AttributeAssignmentType }
     *
     *
     */
    public List<AttributeAssignmentType> getAttributeAssignment() {
        if( attributeAssignment == null ) {
            attributeAssignment = new ArrayList<>();
        }
        return this.attributeAssignment;
    }

    /**
     * Recupera il valore della proprietà adviceId.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAdviceId() {
        return adviceId;
    }

    /**
     * Imposta il valore della proprietà adviceId.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAdviceId( String value ) {
        this.adviceId = value;
    }

}
