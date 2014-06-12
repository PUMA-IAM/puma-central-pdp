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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import mdc.xacml.impl.DefaultAttributeCounter;
import mdc.xacml.impl.HardcodedEnvironmentAttributeModule;
import mdc.xacml.impl.SimplePolicyFinderModule;
import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;
import puma.piputils.QueryAttributeFinderModule;

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
import com.sun.xacml.support.finder.PolicyReader;

/**
 * Class used for evaluating one of multiple policies, based on their id.
 * Internally, this PDP builds multiple SimplePDPs.
 * 
 * @author Maarten Decat
 * 
 */
public class SinglePolicyPDP {

	private static final Logger logger = Logger.getLogger(SinglePolicyPDP.class
			.getName());

	private static final String CENTRAL_POLICY_ID = "global-puma-policy";

	private PDP pdp;

	/**
	 * Initialize this MultiPolicyPDP with given collection of input streams
	 * pointing to XACML policies (XML files).
	 */
	public SinglePolicyPDP(InputStream applicationPolicyStream) {
		// Now setup the attribute finder
		// 1. current date/time
		HardcodedEnvironmentAttributeModule envAttributeModule = new HardcodedEnvironmentAttributeModule();
		// 2. selector module for access to request
		// SelectorModule selectorAttributeModule = new SelectorModule();
		// 3. our own attribute finder module
		QueryAttributeFinderModule localAttributeFinderModule = new QueryAttributeFinderModule();
		// 5. Put everything in an attribute finder
		AttributeFinder attributeFinder = new AttributeFinder();
		List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
		attributeModules.add(envAttributeModule);
		attributeModules.add(localAttributeFinderModule);
		// attributeModules.add(selectorAttributeModule);
		attributeFinder.setModules(attributeModules);

		// build the PDP
		PolicyReader reader = new PolicyReader(null);
		AbstractPolicy policy;
		try {
			policy = reader.readPolicy(applicationPolicyStream);
		} catch (ParsingException e) {
			logger.log(Level.SEVERE, "Error when parsing application policy", e);
			return;
		}
		if (!policy.getId().toString().equals(CENTRAL_POLICY_ID)) {
			logger.severe("The id of the given policy should be \""
					+ CENTRAL_POLICY_ID + "\". Given id: \""
					+ policy.getId().toString() + "\".");
			return;
		}

		// construct the policy finder for the single policy
		PolicyFinder policyFinder = new PolicyFinder();
		SimplePolicyFinderModule simplePolicyFinderModule = new SimplePolicyFinderModule(
				policy);
		Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
		policyModules.add(simplePolicyFinderModule);
		policyFinder.setModules(policyModules);
		this.pdp = new PDP(new PDPConfig(attributeFinder, policyFinder, null,
				null, new DefaultAttributeCounter()));
	}

	/**
	 * Returns the list of supported policy ids.
	 */
	public List<String> getSupportedPolicyIds() {
		List<String> result = new ArrayList<String>();
		result.add(CENTRAL_POLICY_ID);
		return result;
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request) {
		return evaluate(request, new LinkedList<CachedAttribute>());
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) {
		// if supported, evaluate the appropriate policy
		BasicEvaluationCtx ctx;
		try {
			ctx = new BasicEvaluationCtx(request,
					this.pdp.getAttributeFinder(),
					this.pdp.getRemotePolicyEvaluator(),
					new DefaultAttributeCounter());
		} catch (ParsingException e) {
			logger.log(Level.SEVERE, "Parsing exception here??", e);
			return null;
		}
		// add the given cached attributes
		ctx.addAttributesToCache(cachedAttributes);
		// evaluate
		ResponseCtx response = this.pdp.evaluate(ctx);
		return response;
	}
}
