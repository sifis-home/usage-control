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
package it.cnr.iit.ucs.constants;

/**
 * Possible purposes of messages
 *
 * @author Antonio La Marra
 */
public enum PURPOSE {
    TRY,
    START,
    END,
    REEVALUATION,
    REVOKE,
    TRY_RESPONSE,
    START_RESPONSE,
    END_RESPONSE,
    REEVALUATION_RESPONSE,
    ATTRIBUTE_RETRIEVAL,
    ATTRIBUTE_RETRIEVAL_RESPONSE,
    REGISTER,
    REGISTER_RESPONSE,
    ADD_POLICY,
    ADD_POLICY_RESPONSE,
    DELETE_POLICY,
    DELETE_POLICY_RESPONSE,
    LIST_POLICIES,
    LIST_POLICIES_RESPONSE,
    GET_POLICY,
    GET_POLICY_RESPONSE,
    ERROR_RESPONSE
}
