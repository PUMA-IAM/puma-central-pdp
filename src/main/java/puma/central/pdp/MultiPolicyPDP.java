package puma.central.pdp;

import java.io.InputStream;
import java.util.ArrayList;
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
import puma.central.pdp.attr.LocalAttributeFinderModule;

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
 * Class used for evaluating multiple policies, based on their id.
 * Internally, this PDP contains a policy finder with a finder module for each separate policy.
 * 
 * @author jasper
 *
 */
public class MultiPolicyPDP {
	private static final Logger logger = Logger.getLogger(MultiPolicyPDP.class
			.getName());

	private static final String GLOBAL_POLICY_ID = "global-puma-policy";

	private Map<String, AbstractPolicy> policies;
	private PDP pdp;

	/**
	 * Initialize this MultiPolicyPDP with given collection of input streams
	 * pointing to XACML policies (XML files).
	 */
	public MultiPolicyPDP(List<InputStream> policyStreams) {
		// Now setup the attribute finder
		// 1. current date/time
		HardcodedEnvironmentAttributeModule envAttributeModule = new HardcodedEnvironmentAttributeModule();
		// 2. selector module for access to request
		// SelectorModule selectorAttributeModule = new SelectorModule();
		// 3. our own attribute finder module
		LocalAttributeFinderModule localAttributeFinderModule = new LocalAttributeFinderModule();
		// 5. Put everything in an attribute finder
		AttributeFinder attributeFinder = new AttributeFinder();
		List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
		attributeModules.add(envAttributeModule);
		attributeModules.add(localAttributeFinderModule);
		// attributeModules.add(selectorAttributeModule);
		attributeFinder.setModules(attributeModules);

		// build the PDP
		PolicyReader reader = new PolicyReader(null);
		this.policies = new HashMap<String, AbstractPolicy>();
		Boolean containsGlobalPolicy = false;
		try {
			AbstractPolicy policy;
			for (InputStream next: policyStreams) {
				policy = reader.readPolicy(next);
				this.policies.put(policy.getId().toString(), policy);
				if (policy.getId().toASCIIString().equals(GLOBAL_POLICY_ID))
					containsGlobalPolicy = true;
			}
		} catch (ParsingException e) {
			logger.log(Level.SEVERE, "Error when parsing application policy", e);
			return;
		}
		if (!containsGlobalPolicy) {
			logger.severe("The id of the given policy should be \""
					+ GLOBAL_POLICY_ID + "\". Given: "
					+ policies.size() + " policies.");
			return;
		}

		// construct the policy finder for the single policy
		PolicyFinder policyFinder = new PolicyFinder();
		Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
		for (AbstractPolicy next: this.policies.values()) {
			SimplePolicyFinderModule simplePolicyFinderModule = new SimplePolicyFinderModule(next);			
			policyModules.add(simplePolicyFinderModule);
		}
		policyFinder.setModules(policyModules);
		this.pdp = new PDP(new PDPConfig(attributeFinder, policyFinder, null,
				null, new DefaultAttributeCounter()));
	}

	/**
	 * Returns the list of supported policy ids.
	 */
	public List<String> getSupportedPolicyIds() {
		return new ArrayList<String>(this.policies.keySet());
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
