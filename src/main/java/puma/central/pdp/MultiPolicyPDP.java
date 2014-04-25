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
import puma.central.pdp.attr.EntityDatabase;
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
import com.sun.xacml.remote.RemotePolicyEvaluator;
import com.sun.xacml.support.finder.PolicyReader;

/**
 * Class used for evaluating multiple policies, based on their id.
 * Internally, this PDP contains a policy finder with a finder module for each separate policy.
 * 
 * @author jasper
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
	 * 
	 */
	//private PolicyFinder policyFinder;
	
	/**
	 * 
	 */
	private MultiPolicyPolicyFinderModule allPoliciesModule;
	
	/**
	 * The map of ids -> policy objects.
	 */
	private Map<String, AbstractPolicy> policiesById = new HashMap<String, AbstractPolicy>();
	
	/**
	 * Initialize this MultiPolicyPDP with given collection of input streams
	 * pointing to XACML policies (XML files).
	 */
	public MultiPolicyPDP(List<InputStream> policies) {
		// First build the policy finder (to be initialized later)
		//policyFinder = new PolicyFinder();
        allPoliciesModule = new MultiPolicyPolicyFinderModule();
		
		this.buildPolicyMap(policies);

        // Now setup the attribute finder
        // 1. current date/time
        HardcodedEnvironmentAttributeModule envAttributeModule = new HardcodedEnvironmentAttributeModule();
        // 2. selector module for access to request 
        //SelectorModule selectorAttributeModule = new SelectorModule();
        // 3. our own attribute finder module
        LocalAttributeFinderModule localAttributeFinderModule = new LocalAttributeFinderModule();
        // 5. Put everything in an attribute finder
        attributeFinder = new AttributeFinder();
        List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
        attributeModules.add(envAttributeModule);
        //attributeModules.add(selectorAttributeModule);
        attributeModules.add(localAttributeFinderModule);
        attributeFinder.setModules(attributeModules);
        
        // Also set up the remote policy evaluator
        remotePolicyEvaluator = new RemotePolicyEvaluator();
//        Set<RemotePolicyEvaluatorModule> remotePolicyEvaluatorModules = new HashSet<RemotePolicyEvaluatorModule>();
//        remotePolicyEvaluatorModules.add(new TenantPolicyEvaluatorModule());
//        remotePolicyEvaluator.setModules(remotePolicyEvaluatorModules);
        
		// Also set up the general policy finder
//		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
//		for(AbstractPolicy policy: policiesById.values()) {
//			policyFinderModules.add(new SimplePolicyFinderModule(policy));
//		}
//		policyFinder.setModules(policyFinderModules);
        allPoliciesModule.addPolicies(policiesById.values());
        
        EntityDatabase.getInstance().open(true);
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
		AbstractPolicy target = policiesById.get(policyId);
		if(target == null) {
			logger.warning("No policy found for id " + policyId);
			return null;
		}
		//  The correct configuration of PolicyFinderModules:
		// 	At the top policy evaluation, a policy is searched for using findPolicy(context).
		// 	For this request, we add the SimplePolicyFinderModule: this one is only able to find a policy
		//	based on a Context object and just returns the configured single top-level policy (the 
		//	policy with id policyId.
		// 	A policy reference in the top policy is searched for using findPolicy(id, ...).
		//	For these requests, we add the allPoliciesModule: this one is only able to find a policy 
		// 	based on id and can locate all the policies in the policy dir.
		Set<PolicyFinderModule> modules = new HashSet<PolicyFinderModule>();
		modules.add(new SimplePolicyFinderModule(target));
		modules.add(allPoliciesModule);
		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(modules);
        // construct the policy finder for the single policy
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
			Set<PolicyFinderModule> modules = new HashSet<PolicyFinderModule>();
			modules.add(allPoliciesModule);
			PolicyFinder policyFinder = new PolicyFinder();
			policyFinder.setModules(modules);		
			
			PolicyReader reader = new PolicyReader(policyFinder);
			AbstractPolicy policy;
			try {
				policy = reader.readPolicy(stream);
				policiesById.put(policy.getId().toString(), policy);
				logger.info("Added " + policy.getId().toString() + "");
			} catch(ParsingException e) {
				System.err.println("FAILED:");
				e.printStackTrace();
				return;
			}
		}		
	}
}
//public class MultiPolicyPDP {
//	private static final Logger logger = Logger.getLogger(MultiPolicyPDP.class
//			.getName());
//
//	private static final String GLOBAL_POLICY_ID = "global-puma-policy";
//
//	private Map<String, AbstractPolicy> policies;
//	private PDP pdp;
//
//	/**
//	 * Initialize this MultiPolicyPDP with given collection of input streams
//	 * pointing to XACML policies (XML files).
//	 */
//	public MultiPolicyPDP(List<InputStream> policyStreams) {
//		// Now set up the attribute finder
//		// 1. current date/time
//		HardcodedEnvironmentAttributeModule envAttributeModule = new HardcodedEnvironmentAttributeModule();
//		// 2. selector module for access to request
//		// SelectorModule selectorAttributeModule = new SelectorModule();
//		// 3. our own attribute finder module
//		LocalAttributeFinderModule localAttributeFinderModule = new LocalAttributeFinderModule();
//		// 5. Put everything in an attribute finder
//		AttributeFinder attributeFinder = new AttributeFinder();
//		List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
//		attributeModules.add(envAttributeModule);
//		attributeModules.add(localAttributeFinderModule);
//		// attributeModules.add(selectorAttributeModule);
//		attributeFinder.setModules(attributeModules);
//		
//		// Also set up the policy finder
//		PolicyFinder policyFinder = new PolicyFinder();
//		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
//		policyFinderModules.add(new MultiPolicyPolicyFinderModule(policyStreams));
//		policyFinder.setModules(policyFinderModules);
//
//		// build the PDP
//		PolicyReader reader = new PolicyReader(policyFinder);
//		this.policies = new HashMap<String, AbstractPolicy>();
//		Boolean containsGlobalPolicy = false;
//		try {
//			AbstractPolicy policy;
//			for (InputStream next: policyStreams) {
//				policy = reader.readPolicy(next);
//				this.policies.put(policy.getId().toString(), policy);
//				if (policy.getId().toASCIIString().equals(GLOBAL_POLICY_ID))
//					containsGlobalPolicy = true;
//			}
//		} catch (ParsingException e) {
//			logger.log(Level.SEVERE, "Error when parsing application policy", e);
//			return;
//		}
//		if (!containsGlobalPolicy) {
//			logger.severe("The id of the given policy should be \""
//					+ GLOBAL_POLICY_ID + "\". Given: "
//					+ policies.size() + " policies.");
//			return;
//		}
//		
//		this.pdp = new PDP(new PDPConfig(attributeFinder, policyFinder, null,
//				null, new DefaultAttributeCounter()));
//	}
//
//	/**
//	 * Returns the list of supported policy ids.
//	 */
//	public List<String> getSupportedPolicyIds() {
//		return new ArrayList<String>(this.policies.keySet());
//	}
//
//	/**
//	 * Evaluate a request and return the result.
//	 */
//	public ResponseCtx evaluate(String policyId, RequestType request) {
//		return evaluate(policyId, request, new LinkedList<CachedAttribute>());
//	}
//
//	/**
//	 * Evaluate a request and return the result.
//	 */
//	public ResponseCtx evaluate(String policyId, RequestType request,
//			List<CachedAttribute> cachedAttributes) {
//		// if supported, evaluate the appropriate policy
//		BasicEvaluationCtx ctx;
//		try {
//			ctx = new BasicEvaluationCtx(request,
//					this.pdp.getAttributeFinder(),
//					this.pdp.getRemotePolicyEvaluator(),
//					new DefaultAttributeCounter());
//		} catch (ParsingException e) {
//			logger.log(Level.SEVERE, "Parsing exception here??", e);
//			return null;
//		}
//		// add the given cached attributes
//		ctx.addAttributesToCache(cachedAttributes);
//		// evaluate
//		ResponseCtx response = this.pdp.evaluate(ctx);
//		return response;
//	}
//}
