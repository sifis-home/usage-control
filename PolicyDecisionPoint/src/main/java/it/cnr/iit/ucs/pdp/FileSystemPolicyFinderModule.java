/*
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package it.cnr.iit.ucs.pdp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.MatchResult;
import org.wso2.balana.Policy;
import org.wso2.balana.PolicyMetaData;
import org.wso2.balana.PolicyReference;
import org.wso2.balana.PolicySet;
import org.wso2.balana.VersionConstraints;
import org.wso2.balana.combine.PolicyCombiningAlgorithm;
import org.wso2.balana.combine.xacml2.DenyOverridesPolicyAlg;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ctx.Status;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.PolicyFinderResult;

import it.cnr.iit.ucs.exceptions.PolicyException;
import it.cnr.iit.utility.FileUtility;
import it.cnr.iit.xacml.wrappers.PolicyWrapper;

/**
 * This is a filesystem policy repository.
 */

public class FileSystemPolicyFinderModule extends PolicyFinderModule {

    private static final Logger log = Logger.getLogger( FileSystemPolicyFinderModule.class.getName() );

    private PolicyFinder finder = null;

    private static final String POLICY_FILE_EXTENSION = ".xml";

    private String POLICY_FILE_FOLDER = null;

    private Map<URI, AbstractPolicy> policies;

    private PolicyCombiningAlgorithm combiningAlg;

    private DocumentBuilderFactory documentBuilderFactory;

    public static final String POLICY_REPOSITORY_DB_PROPERTY = "com.huawei.policy.decision.PolicyRepository";

    public FileSystemPolicyFinderModule(String policyFolderPath, String status) {
        policies = new HashMap<URI, AbstractPolicy>();
        POLICY_FILE_FOLDER = policyFolderPath;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );
            documentBuilderFactory.setIgnoringComments( true );
            documentBuilderFactory.setNamespaceAware( true );
            documentBuilderFactory.setValidating( false );
        } catch( Exception e ) {
            log.severe( e.getMessage() );
            throw new IllegalStateException( "Unable to protect against XXE" );
        }
        loadPolicies(status);
    }

    @Override
    public void init(PolicyFinder finder) {
        this.finder = finder;
        combiningAlg = new DenyOverridesPolicyAlg();
    }

    @Override
    public PolicyFinderResult findPolicy(EvaluationCtx context) {

        ArrayList<AbstractPolicy> selectedPolicies = new ArrayList<AbstractPolicy>();
        Set<Map.Entry<URI, AbstractPolicy>> entrySet = policies.entrySet();

        // iterate through all the policies we currently have loaded
        for (Map.Entry<URI, AbstractPolicy> entry : entrySet) {

            AbstractPolicy policy = entry.getValue();
            MatchResult match = policy.match(context);
            int result = match.getResult();

            // if target matching was indeterminate, then return the error
            if (result == MatchResult.INDETERMINATE)
                return new PolicyFinderResult(match.getStatus());

            // see if the target matched
            if (result == MatchResult.MATCH) {

                if ((combiningAlg == null) && (selectedPolicies.size() > 0)) {
                    // we found a match before, so this is an error
                    ArrayList<String> code = new ArrayList<String>();
                    code.add(Status.STATUS_PROCESSING_ERROR);
                    Status status = new Status(code, "too many applicable " + "top-level policies");
                    return new PolicyFinderResult(status);
                }

                // this is the first match we've found, so remember it
                selectedPolicies.add(policy);
            }
        }

        // no errors happened during the search, so now take the right
        // action based on how many policies we found
        switch (selectedPolicies.size()) {
        case 0:
            log.info("No matching XACML policy found");
            return new PolicyFinderResult();
        case 1:
            return new PolicyFinderResult((selectedPolicies.get(0)));
        default:
            return new PolicyFinderResult(new PolicySet(null, combiningAlg, null, selectedPolicies));
        }
    }

    @Override
    public PolicyFinderResult findPolicy(URI idReference, int type, VersionConstraints constraints,
            PolicyMetaData parentMetaData) {

        AbstractPolicy policy = policies.get(idReference);
        if (policy != null) {
            if (type == PolicyReference.POLICY_REFERENCE) {
                if (policy instanceof Policy) {
                    return new PolicyFinderResult(policy);
                }
            } else {
                if (policy instanceof PolicySet) {
                    return new PolicyFinderResult(policy);
                }
            }
        }

        // if there was an error loading the policy, return the error
        ArrayList<String> code = new ArrayList<String>();
        code.add(Status.STATUS_PROCESSING_ERROR);
        Status status = new Status(code, "couldn't load referenced policy");
        return new PolicyFinderResult(status);
    }

    @Override
    public boolean isIdReferenceSupported() {
        return true;
    }

    @Override
    public boolean isRequestSupported() {
        return true;
    }

    /**
     * Re-sets the policies known to this module to those contained in the given
     * database.
     * 
     * @return
     */
    public Integer loadPolicies(String status) {
        // Load all policy from fs folder
        policies.clear();
        if (POLICY_FILE_FOLDER == null) {
            throw new IllegalStateException( "Policy folder path not set." );            
        }
        try {
            Files.walk(Paths.get( POLICY_FILE_FOLDER ))
            .filter(Files::isRegularFile)
            .map(Path::toString)
            .filter(path -> path.endsWith( POLICY_FILE_EXTENSION ))
            .map(policy -> {
                try {
                    return PolicyWrapper.build(FileUtility.readFileAsString(policy))
                            .getPolicyForCondition(status).getPolicy();
                } catch (PolicyException e) { return null; }
            })
            .filter(Objects::nonNull)
            .forEach(policy -> loadPolicy(policy, finder));
        } catch (IOException e) {
            throw new IllegalStateException( "Unable to read policies from filesystem" );
        }
        
        return policies.size();
    }

    /**
     * Private helper that tries to load the given policy, and returns null if any
     * error occurs.
     *
     * @param policy      file path to policy
     * @param finder      policy finder
     * @return org.w3c.dom.Element
     */
    protected AbstractPolicy loadPolicy(String policy, PolicyFinder finder) {
        AbstractPolicy abstractPolicy = null;

        try (InputStream stream = new ByteArrayInputStream( policy.getBytes() )) {
            DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
            Document doc = db.parse( stream );
            Element root = doc.getDocumentElement();
            String name = root.getLocalName();

            if( name.equals( "Policy" ) ) {
                abstractPolicy = Policy.getInstance( root );
            } else if( name.equals( "PolicySet" ) ) {
                abstractPolicy = PolicySet.getInstance( root, finder );
            }
        } catch( Exception e ) {
            log.severe( "fail to load UXACML policy : " + e.getLocalizedMessage() );
        }

        if( abstractPolicy != null ) {
            policies.put( abstractPolicy.getId(), abstractPolicy );
        }

        return abstractPolicy;

    }

}
