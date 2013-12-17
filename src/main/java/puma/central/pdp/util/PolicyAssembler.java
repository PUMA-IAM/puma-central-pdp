package puma.central.pdp.util;

import java.util.List;
import java.util.logging.Logger;

public class PolicyAssembler {
	private static final Logger logger = Logger.getLogger(PolicyAssembler.class.getName());
	private static final String DEFAULT_ENCODING = "UTF-8";
	public static final String DEFAULT_URI = "urn:xacml:2.0:puma:tenantsetid:";
	private String initialPolicy;
	private String policyDir;
	
	private static final String POLICY_HEADER = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n " +
			"<PolicySet  xmlns=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\"\n" +
            "PolicySetId=\"global-puma-policy\"\n" +
            "PolicyCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides\">\n" +
            "<Description>The global access control policy</Description>\n" + 
            "<PolicySetIdReference>central-puma-policy</PolicySetIdReference>\n";
	private static final String POLICY_FOOTER = "</PolicySet>\n";
	
	/*public PolicyAssembler(String directory, String policy) {
		this.policyDir = directory;
		this.initialPolicy = policy;
	}*/
	
	public PolicyAssembler() {
		
	}
	
	public String assemble(List<String> tenantIdentifiers) {
		/* , OutputStream outStream
		 * 
		 * try {
			InputStream istream = new ByteArrayInputStream(this.initialPolicy.getBytes(DEFAULT_ENCODING));
			PolicyReader reader = new PolicyReader(null);
			AbstractPolicy sourcePolicy = reader.readPolicy(istream);
			PolicySet finalPolicy = new PolicySet(sourcePolicy.getId(), sourcePolicy.getVersion(), (PolicyCombiningAlgorithm) sourcePolicy.getCombiningAlg(), sourcePolicy.getDescription(), sourcePolicy.getTarget(), this.getTenantPolicies(tenantIdentifiers), sourcePolicy.getDefaultVersion());
			finalPolicy.encode(outStream); 
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Could not parse policy", e);
		} catch (ParsingException e) {
			logger.log(Level.SEVERE, "Could not parse policy", e);
		} catch (IllegalArgumentException e) {
			logger.log(Level.SEVERE, "Could not parse policy", e);
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, "Could not parse policy", e);
		}
		try {
			outStream.write(this.initialPolicy.getBytes(DEFAULT_ENCODING));
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Could not write policy", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not write policy", e);
		}*/
		String result = POLICY_HEADER;
		result = result + this.getTenantPolicies(tenantIdentifiers);
		result = result + POLICY_FOOTER;
		return result;
	}

	private String getTenantPolicies(List<String> tenantIdentifiers) { //throws IllegalArgumentException, URISyntaxException {
		/* Return List<AbstractPolicy>
		 * 
		 * PolicyFinder finder = new PolicyFinder();
		List<AbstractPolicy> result = new ArrayList<AbstractPolicy>(tenantIdentifiers.size());
		List<String> fileNames = new ArrayList<String>(tenantIdentifiers.size());
		PolicyFinderModule module = null;
		for (String next: tenantIdentifiers) {
			fileNames.add(this.policyDir + next + ".xml");
			result.add(new PolicyReference(this.buildReference(next), PolicyReference.POLICYSET_REFERENCE, null, finder, null));
		}
		module = new FilePolicyModule(fileNames);
		module.init(finder);*/
		String result = "";
		for (String next: tenantIdentifiers) {
			result = result + "<PolicySetIdReference>" + DEFAULT_URI + next  + "</PolicySetIdReference>\n";
		}
		return result;
	}

/*	private URI buildReference(String next) throws URISyntaxException {
		return new URI(DEFAULT_URI + next);
	}*/
}
