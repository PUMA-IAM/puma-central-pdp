package puma.centralpdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;
import puma.centralpdp.rmi.CentralPUMAPDPRemote;

import com.sun.xacml.ctx.CachedAttribute;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

/**
 * The main (only) access point for the central PUMA PDP from the application
 * PDP(s).
 * 
 * NOTICE: the PDP just evaluates the policy with id "central-puma-policy", so
 * make sure this one is present and know that the rest will not be evaluated.
 * 
 * @author Maarten Decat
 * 
 */
public class CentralPUMAPDP implements CentralPUMAPDPRemote {

	public static void main(String[] args) {
		/**
		 * STARTUP THE RMI SERVER
		 */
		// if (System.getSecurityManager() == null) {
		// System.setSecurityManager(new SecurityManager());
		// }
		try {
			CentralPUMAPDP pdp = new CentralPUMAPDP();
			CentralPUMAPDPRemote stub = (CentralPUMAPDPRemote) UnicastRemoteObject
					.exportObject(pdp, 0);
			Registry registry = LocateRegistry.createRegistry(2020);
			registry.bind("central-puma-pdp", stub);
			System.out
					.println("Central PUMA PDP up and running (available using RMI with name \"central-puma-pdp\")");
		} catch (Exception e) {
			System.err.println("FAILED to set up PDP as RMI server:");
			e.printStackTrace();
		}
	}

	private static final String CENTRAL_POLICY_ID = "central-puma-policy";

	private MultiPolicyPDP pdp;

	public CentralPUMAPDP() {
		this.initializePDP(getPolicyStreams());
		if (!this.pdp.supportsPolicyId(CENTRAL_POLICY_ID)) {
			throw new IllegalArgumentException(
					"The application policy was not found (should have id \""
							+ CENTRAL_POLICY_ID + "\")");
		}
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) throws RemoteException {
		ResponseCtx response = this.pdp.evaluate(CENTRAL_POLICY_ID, request,
				cachedAttributes);

		// print out some information
		String msg = "Results in response to request : ";
		for (Object r : response.getResults()) {
			Result result = (Result) r;
			msg += "(status: " + result.getStatus() + ", result: "
					+ result.getHumanReadableDecision() + ", #obligations: "
					+ result.getObligations().size() + ") ";
		}
		System.out.println(msg);
		
		return response;
	}

	/**
	 * Initialize the application PDP with the given list of policy files
	 * (represented by their respective InputStreams).
	 * 
	 * This method should be called before the first call to isAuthorized()
	 * 
	 * @param policyDir
	 */
	private void initializePDP(Collection<InputStream> policies) {
		this.pdp = new MultiPolicyPDP(policies);
		// check that "application-pdp" is present
		if (!this.pdp.getSupportedPolicyIds().contains(CENTRAL_POLICY_ID)) {
			throw new InvalidParameterException(
					"The application policy was not found (should have id \""
							+ CENTRAL_POLICY_ID + "\")");
		}
	}

	private static Collection<InputStream> getPolicyStreams() {
		// Note: a servlet should use context.getResourcePaths("/policies")
		File dir = new File(
				"/home/maartend/PhD/code/workspace-jee/puma-central-puma-pdp/resources/policies/");
		List<File> files = Arrays.asList(dir.listFiles());
		if (files.isEmpty()) {
			throw new RuntimeException("No policies found, exiting.");
		}
		List<InputStream> policies = new ArrayList<InputStream>();
		for (File file : files) {
			if (file.isFile() && !file.getName().endsWith("~")) {
				// can be a directory as well
				try {
					policies.add(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					// should never happen
					e.printStackTrace();
				}
			}
		}
		return policies;
	}

}
