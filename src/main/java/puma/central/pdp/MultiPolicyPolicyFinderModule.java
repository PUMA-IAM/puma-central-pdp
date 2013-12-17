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
