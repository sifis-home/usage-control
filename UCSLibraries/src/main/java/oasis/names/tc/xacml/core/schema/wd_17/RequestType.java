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
package oasis.names.tc.xacml.core.schema.wd_17;

import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequestType",
        propOrder = {"requestDefaults", "attributes", "multiRequests"})
public final class RequestType {

    private static final Logger log = Logger.getLogger(RequestType.class.getName());

    @XmlElement(name = "RequestDefaults")
    protected RequestDefaultsType requestDefaults;
    @XmlElement(name = "Attributes", required = true)
    protected List<AttributesType> attributes;
    @XmlElement(name = "MultiRequests")
    protected MultiRequestsType multiRequests;
    @XmlAttribute(name = "ReturnPolicyIdList", required = true)
    protected boolean returnPolicyIdList;
    @XmlAttribute(name = "CombinedDecision", required = true)
    protected boolean combinedDecision;

    public RequestDefaultsType getRequestDefaults() {
        return requestDefaults;
    }

    public void setRequestDefaults(RequestDefaultsType value) {
        this.requestDefaults = value;
    }

    public List<AttributesType> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return this.attributes;
    }

    public MultiRequestsType getMultiRequests() {
        return multiRequests;
    }

    public void setMultiRequests(MultiRequestsType value) {
        this.multiRequests = value;
    }

    public boolean isReturnPolicyIdList() {
        return returnPolicyIdList;
    }

    public void setReturnPolicyIdList(boolean value) {
        this.returnPolicyIdList = value;
    }

    public boolean isCombinedDecision() {
        return combinedDecision;
    }

    public void setCombinedDecision(boolean value) {
        this.combinedDecision = value;
    }

    /**
     * Given a category, retrieve the attribute value of either the subject-id,
     * the resource-id, or the action-id from the request.
     * The method first extracts the attributes of the provided category and
     * then filters them to get the value of either the subject-id, resource-id,
     * or action-id, based on the provided category. If that attribute is not
     * found, it returns an empty string.
     * Note that this function assumes that the attribute has only one value.
     * Technically, it returns only the value of first element of the
     * AttributeType object.
     *
     * @param category the category we are interested into
     * @return the value of the attribute, an empty String otherwise
     */
    public String getAttributeValue(Category category) {
        try {
            // select the attributes of category 'category'
            AttributesType attributesType = attributes.stream()
                    .filter(attrs -> category.toString().equals(attrs.getCategory())).findAny().orElse(null);
            if (attributesType == null) return "";

            // select the attributeId based on the category
            String attributeId;
            if (category.equals(Category.SUBJECT)) {
                attributeId = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
            } else if (category.equals(Category.RESOURCE)) {
                attributeId = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
            } else if (category.equals(Category.ACTION)) {
                attributeId = "urn:oasis:names:tc:xacml:1.0:action:action-id";
            } else {
                return "";
            }

            final String attrId = attributeId;

            // get the value of the attribute with category 'category' and attributeId as selected above
            AttributeType attributeType = attributesType.getAttribute().stream()
                    .filter(attr -> attrId.equals(attr.getAttributeId())).findAny().orElse(null);
            if (attributeType == null) return "";

            return attributeType.getAttributeValue().get(0).getContent().get(0).toString();

        } catch (Exception e) {
            log.severe("error getting attribute value : " + e.getMessage());
        }

        return "";
    }

    public String getAttribute(String category, String attributeId) {
        String res = null;

        AttributesType attbs = attributes.stream()
                .filter(a -> a.getCategory().endsWith(category)).findFirst()
                .orElse(null);

        if (attbs != null) {
            AttributeType attr = attbs.getAttribute().stream()
                    .filter(a -> a.getAttributeId().endsWith(attributeId)).findFirst()
                    .orElse(null);

            if (attr != null) {
                res = attr.getAttributeValue().get(0).getContent().get(0).toString();
            }
        }

        return res;
    }

    public boolean addAttribute(Attribute attribute, String value) {
        return addAttribute(attribute.getCategory().toString(),
                attribute.getDataType().toString(),
                attribute.getAttributeId(), value);
    }

    public boolean removeAttribute(Category category, String attributeId) {
        return this.attributes.removeIf(a -> Objects.equals(a.getCategory(), category.toString()) && Objects.equals(a.getId(), attributeId));
    }

    public boolean addAttribute(String category, String dataType,
                                String attributeId, String value) {

        AttributeValueType attributeValueType = new AttributeValueType();
        attributeValueType.setDataType(dataType);
        attributeValueType.getContent().add(value);
        AttributeType attributeType = new AttributeType();
        attributeType.setAttributeId(attributeId);
        attributeType.setIncludeInResult(false);
        attributeType.setIssuer("false");
        attributeType.getAttributeValue().add(attributeValueType);
        AttributesType attributesType = new AttributesType();
        attributesType.setCategory(category);
        attributesType.getAttribute().add(attributeType);
        boolean added = false;
        for (AttributesType at : this.getAttributes()) {
            if (at.getCategory().equals(category)) {
                at.getAttribute().add(attributeType);
                added = true;
            }
        }
        if (!added) {
            this.getAttributes().add(attributesType);
        }

        return true;
    }

    public boolean addAttribute(String category, String dataType,
                                String attributeId, List<String> value) {
        AttributeValueType attributeValueType = new AttributeValueType();
        attributeValueType.setDataType(dataType);
        attributeValueType.getContent().addAll(value);
        AttributeType attributeType = new AttributeType();
        attributeType.setAttributeId(attributeId);
        attributeType.setIncludeInResult(false);
        attributeType.setIssuer("false");
        attributeType.getAttributeValue().add(attributeValueType);
        AttributesType attributesType = new AttributesType();
        attributesType.setCategory(category);
        attributesType.getAttribute().add(attributeType);
        boolean added = false;
        for (AttributesType at : this.getAttributes()) {
            if (at.getCategory().equals(category)) {
                at.getAttribute().add(attributeType);
                added = true;
            }
        }
        if (!added) {
            this.getAttributes().add(attributesType);
        }

        return true;
    }

}
