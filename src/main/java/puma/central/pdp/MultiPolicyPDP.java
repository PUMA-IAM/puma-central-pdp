/*******************************************************************************
 * Copyright 2013 KU Leuven Research and Developement - IBBT - Distrinet 
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import mdc.xacml.impl.DefaultAttributeCounter;
import mdc.xacml.impl.HardcodedEnvironmentAttributeModule;
import mdc.xacml.impl.SimplePolicyFinderModule;
import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;
import puma.central.pdp.tenant.TenantPolicyEvaluatorModule;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.BasicEvaluationCtx;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.ctx.CachedAttribute;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.remote.RemotePolicyEvaluator;
import com.sun.xacml.remote.RemotePolicyEvaluatorModule;
import com.sun.xacml.support.finder.PolicyReader;

/**
 * Class used for evaluating one of multiple policies, based on their id.
 * Internally, this PDP builds multiple SimplePDPs.
 * 
 * @author Maarten Decat
 *
 */
public class MultiPolicyPDP {
	
	private static final Logger logger = Logger
			.getLogger(MultiPolicyPDP.class.getName());
	
	/**
	 * The attribute finder does not have to be constructed for every policy request.
	 * Store it in this variable.
	 */
	private AttributeFinder attributeFinder;
	
	/**
	 * The remote policy evaluator does not have to be constructed for every policy request.
	 * Store it in this variable.
	 */
	private RemotePolicyEvaluator remotePolicyEvaluator; 
	
	/**
	 * The map of ids -> policy objects.
	 */
	private Map<String, AbstractPolicy> policiesById = new HashMap<String, AbstractPolicy>();
	
	/**
	 * Initialize this MultiPolicyPDP with given collection of input streams
	 * pointing to XACML policies (XML files).
	 */
	public MultiPolicyPDP(Collection<InputStream> policies) {
		this.buildPolicyMap(policies);

        // Now setup the attribute finder
        // 1. current date/time
        HardcodedEnvironmentAttributeModule envAttributeModule = new HardcodedEnvironmentAttributeModule();
        // 2. selector module for access to request 
        //SelectorModule selectorAttributeModule = new SelectorModule();
        // 3. our own attribute finder module
        //LocalAttributeFinderModule localAttributeFinderModule = new LocalAttributeFinderModule();
        // 5. Put everything in an attribute finder
        attributeFinder = new AttributeFinder();
        List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
        attributeModules.add(envAttributeModule);
        //attributeModules.add(selectorAttributeModule);
        //attributeModules.add(localAttributeFinderModule);
        attributeFinder.setModules(attributeModules);
        
        // Also set up the remote policy evaluator
        remotePolicyEvaluator = new RemotePolicyEvaluator();
        Set<RemotePolicyEvaluatorModule> remotePolicyEvaluatorModules = new HashSet<RemotePolicyEvaluatorModule>();
        remotePolicyEvaluatorModules.add(new TenantPolicyEvaluatorModule());
        remotePolicyEvaluator.setModules(remotePolicyEvaluatorModules);
	}
	
	/**
	 * Returns the list of supported policy ids.
	 */
	public List<String> getSupportedPolicyIds() {
		List<String> result = new ArrayList<String>();
		result.addAll(this.policiesById.keySet());
		return result;
	}
	
	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(String policyId, RequestType request) {
		return evaluate(policyId, request, new LinkedList<CachedAttribute>());
	}
	
	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(String policyId, RequestType request, List<CachedAttribute> cachedAttributes) {
		// first check whether we support the given policy id
		if(!this.supportsPolicyId(policyId)) {
			logger.log(Level.WARNING, "Unsupported policy id requested to be evaluated: " + policyId);
			throw new IllegalArgumentException("Unsupported policy id requested to be evaluated: " + policyId);
		}
		// if supported, evaluate the appropriate policy
		BasicEvaluationCtx ctx;
		try {
			ctx = new BasicEvaluationCtx(request, attributeFinder, null, new DefaultAttributeCounter());
		} catch (ParsingException e) {
			logger.log(Level.SEVERE, "Parsing exception here??", e);
			return null;
		}
		// add the given cached attributes 
		ctx.addAttributesToCache(cachedAttributes);
		// evaluate
		ResponseCtx response = getPDPForPolicy(policyId).evaluate(ctx);
		return response;
	}
	
	/**
	 * Construct the PDP for the given policy.
	 */
	private PDP getPDPForPolicy(String policyId) {
        SimplePolicyFinderModule simplePolicyFinderModule = new SimplePolicyFinderModule(getPolicy(policyId));
        // construct the policy finder for the single policy
        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(simplePolicyFinderModule);
        policyFinder.setModules(policyModules);
		return new PDP(new PDPConfig(attributeFinder, policyFinder, null, null, new DefaultAttributeCounter()));
	}
	
	/**
	 * Returns the requested policy wrapped in a list. Returns an empty list if the policy
	 * cannot be found.
	 */
	public AbstractPolicy getPolicy(String policyId) {
		return policiesById.get(policyId);
	}
	
	/**
	 * Returns whether this PDP contains a policy with given id.
	 */
	public boolean supportsPolicyId(String policyId) {
		return policiesById.containsKey(policyId);
	}
	
	/**
	 * 
	 */
	private void buildPolicyMap(Collection<InputStream> policies) {
		for(InputStream stream: policies) {
			PolicyReader reader = new PolicyReader(null);
			AbstractPolicy policy;
			try {
				policy = reader.readPolicy(stream);
				policiesById.put(policy.getId().toString(), policy);
				logger.info("Added " + policy.getId().toString());
			} catch(ParsingException e) {
				System.err.println("FAILED:");
				e.printStackTrace();
				return;
			}
		}		
	}
}
