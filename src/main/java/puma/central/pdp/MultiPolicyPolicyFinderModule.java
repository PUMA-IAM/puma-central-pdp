/*******************************************************************************
 * Copyright 2014 KU Leuven Research and Developement - iMinds - Distrinet 
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Administrative Contact: dnet-project-office@cs.kuleuven.be
 *    Technical Contact: maarten.decat@cs.kuleuven.be
 *    Author: maarten.decat@cs.kuleuven.be
 ******************************************************************************/
package puma.central.pdp;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.VersionConstraints;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;


/**
 * 
 */
public class MultiPolicyPolicyFinderModule extends PolicyFinderModule {

    // the actual loaded policies
    private Map<String, AbstractPolicy> policies = new HashMap<String, AbstractPolicy>();

    // the logger we'll use for all messages
    private static final Logger logger =
        Logger.getLogger(MultiPolicyPolicyFinderModule.class.getName());

//    /**
//     * 
//     */
//    public MultiPolicyPolicyFinderModule(List<InputStream> policyStreams) {
//		PolicyReader reader = new PolicyReader(null);
//    	for(InputStream is: policyStreams) {
//            try {
//                AbstractPolicy policy =
//                    reader.readPolicy(is);
//                policies.put(policy.getId().toString(), policy);
//            } catch (ParsingException pe) {
//                logger.log(Level.WARNING, "Error reading policy", pe);
//            }
//        }
//    }

    /**
     * 
     */
    public MultiPolicyPolicyFinderModule() {
    	// nothing to do
    }

    /**
     * 
     */
    public MultiPolicyPolicyFinderModule(Collection<AbstractPolicy> inputPolicies) {
    	for(AbstractPolicy policy: inputPolicies) {
    		policies.put(policy.getId().toString(), policy);
        }
    }
    
    /**
     * 
     * @param inputPolicies
     */
    public void addPolicies(Collection<AbstractPolicy> inputPolicies) {
    	for(AbstractPolicy policy: inputPolicies) {
    		policies.put(policy.getId().toString(), policy);
        }
    }

    /**
     * 
     */
    public void init(PolicyFinder finder) {
    	// do nothing
    	// TODO I hope this is OK?
    }

    /**
     * Returns that the module supports finding policies based on an
     * id reference (in a PolicySet).
     *
     * @return true if idReference retrieval is supported
     */
    public boolean isIdReferenceSupported() {
        return true;
    }

    /**
     * 
     */
    public PolicyFinderResult findPolicy(URI idReference, int type,
                                         VersionConstraints constraints,
                                         PolicyMetaData parentMetaData) {
    	AbstractPolicy p = policies.get(idReference.toString());
    	if(p == null) {
    		return new PolicyFinderResult();
    	} else {
    		return new PolicyFinderResult(p);
    	}
    }

}
