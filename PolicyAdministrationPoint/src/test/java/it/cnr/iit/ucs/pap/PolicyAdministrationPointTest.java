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
package it.cnr.iit.ucs.pap;

import it.cnr.iit.ucs.exceptions.PAPException;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.utility.FileUtility;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.*;

public class PolicyAdministrationPointTest {

    private static final String POLICY_FILE_LOCATION = "../res/xmls/";
    private static final String POLICY_ID = "policy2Attributes.xml";
    private static final String POLICY_FILE_NAME_TO_CREATE = "policy_1.xml";
    private PolicyAdministrationPoint pap;

    @Before
    public void init() {
        File policyFile = new File(POLICY_FILE_LOCATION + POLICY_ID);
        if (policyFile.exists()) {
            policyFile.delete();
        }
        pap = new PolicyAdministrationPoint(properties);
    }

    @Test
    public void aPolicyCanBePersistedAndReloaded() {
        // given a policy
        String policyToStore = FileUtility.readFileAsString(POLICY_FILE_LOCATION + POLICY_FILE_NAME_TO_CREATE);

        // when pap store is called
        String policyId = pap.addPolicy(policyToStore);

        // then the policy is persisted
        assertNotNull(policyId);

        // and the policy can be retrieved
        String policyRetrieved = pap.retrievePolicy(policyId);

        // and the retrieved one is same as the one persisted
        assertEquals(policyToStore, policyRetrieved);

        // The policy is deleted
        assertTrue(pap.deletePolicy(policyId));

        // If we try to delete the same policy again, it fails and returns false
        assertFalse(pap.deletePolicy(policyId));
    }

    @Test
    public void policiesCanBeListed() {
        try {
            assertFalse(pap.listPolicies().isEmpty());
        } catch (PAPException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFailListPolicies() {
        PapProperties wrongPathProperties = new PapProperties() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public Map<String, String> getAdditionalProperties() {
                return null;
            }

            @Override
            public String getPath() {
                return "wrong-path";
            }
        };
        PolicyAdministrationPoint pap = new PolicyAdministrationPoint(wrongPathProperties);
        try {
            pap.listPolicies();
        } catch (PAPException e) {
            assertEquals("Error retrieving the list of policies", e.getMessage());
        }
    }

    @Test
    public void testGetPath() {
        assertEquals(POLICY_FILE_LOCATION, pap.getPath());
    }

    @Test
    public void testFailAddPolicy() {
        assertNull(pap.addPolicy("non-valid-policy"));
    }

    @Test
    public void testFailRetrievePolicy() {
        assertNull(pap.retrievePolicy("non-existent-policy-id"));
    }

    private final PapProperties properties = new PapProperties() {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public Map<String, String> getAdditionalProperties() {
            return null;
        }

        @Override
        public String getPath() {
            return POLICY_FILE_LOCATION;
        }
    };
}
