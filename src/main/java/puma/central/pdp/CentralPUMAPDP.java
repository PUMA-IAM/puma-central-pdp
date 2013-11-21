package puma.central.pdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import puma.rmi.pdp.CentralPUMAPDPRemote;
import puma.rmi.pdp.mgmt.CentralPUMAPDPMgmtRemote;

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
public class CentralPUMAPDP implements CentralPUMAPDPRemote, CentralPUMAPDPMgmtRemote {

	private static final String CENTRAL_PUMA_PDP_RMI_NAME = "central-puma-pdp";

	private static final String CENTRAL_PUMA_POLICY_FILENAME = "central-puma-policy.xml";

	private static final int RMI_REGISITRY_PORT = 2040;

	private static final Logger logger = Logger.getLogger(CentralPUMAPDP.class
			.getName());

	public static void main(String[] args) {
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("ph", "policy-home", true,
				"The folder where to find the central puma policy file (called " + CENTRAL_PUMA_POLICY_FILENAME + ")");
		
		String policyHome = "";

		// read command line
		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Simple PDP Test", options);
				return;
			}
			if (line.hasOption("policy-home")) {
				policyHome = line.getOptionValue("policy-home");
			} else {
				logger.log(Level.WARNING, "Incorrect arguments given.");
				return;
			}
		} catch (ParseException e) {
			logger.log(Level.WARNING, "Incorrect arguments given.", e);
			return;
		}
		
		
		//
		// STARTUP THE RMI SERVER
		//		
		// if (System.getSecurityManager() == null) {
		// System.setSecurityManager(new SecurityManager());
		// }

		try {
			Registry registry;
			try {
				registry = LocateRegistry.createRegistry(RMI_REGISITRY_PORT);
				logger.info("Created new RMI registry");
			} catch (RemoteException e) {
				// MDC: I hope this means the registry already existed.
				registry = LocateRegistry.getRegistry(RMI_REGISITRY_PORT);
				logger.info("Reusing existing RMI registry");
			}
			CentralPUMAPDP pdp = new CentralPUMAPDP(policyHome);
			CentralPUMAPDPRemote stub = (CentralPUMAPDPRemote) UnicastRemoteObject
					.exportObject(pdp, 0);
			registry.bind(CENTRAL_PUMA_PDP_RMI_NAME, stub);
			logger.info("Central PUMA PDP up and running (available using RMI with name \"central-puma-pdp\")");
		} catch(Exception e) {
			logger.log(Level.SEVERE, "FAILED to set up PDP as RMI server", e);
		}
	}

	private SinglePolicyPDP pdp;

	public CentralPUMAPDP(String policyDir) throws FileNotFoundException {
		status = "NOT INITIALIZED";
		initializePDP(policyDir);
	}

	private String centralPUMAPolicyFilename;

	private String status;

	/**
	 * Initialize the application PDP by scanning all policy files in the given
	 * directory.
	 * 
	 * This method should be called before the first call to isAuthorized().
	 * 
	 * @param policyDir
	 *            WITH trailing slash.
	 */
	public void initializePDP(String policyDir) throws FileNotFoundException {
		// store for later usage
		this.centralPUMAPolicyFilename = policyDir
				+ CENTRAL_PUMA_POLICY_FILENAME;

		InputStream applicationPolicyStream;
		try {
			applicationPolicyStream = new FileInputStream(
					centralPUMAPolicyFilename);
			logger.info("Using policy file " + centralPUMAPolicyFilename);
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Application policy file not found");
			status = "APPLICATION POLICY FILE NOT FOUND";
			throw e;
		}
		this.pdp = new SinglePolicyPDP(applicationPolicyStream);
		logger.info("initialized application PDP");
		status = "OK";
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) throws RemoteException {
		ResponseCtx response = this.pdp.evaluate(request,
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

	/***********************
	 * APPLICATION PDP MGMT
	 ***********************/

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void loadCentralPUMAPolicy(String policy) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(centralPUMAPolicyFilename, "UTF-8");
		} catch (FileNotFoundException e) {
			logger.log(
					Level.SEVERE,
					"Application policy file not found when writing new Central PUMA PDP policy",
					e);
			return;
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE,
					"Unsupported encoding when writing new Central PUMA PDP policy",
					e);
			return;
		}
		writer.print(policy);
		writer.close();
		logger.info("Succesfully reloaded Central PUMA PDP policy");
		this.reload();
	}

	@Override
	public void reload() {
		// just set up a new PDP
		InputStream applicationPolicyStream;
		try {
			applicationPolicyStream = new FileInputStream(
					centralPUMAPolicyFilename);
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE,
					"Could not reload PDP: Central PUMA PDP policy file not found",
					e);
			status = "APPLICATION POLICY FILE NOT FOUND";
			return;
		}
		this.pdp = new SinglePolicyPDP(applicationPolicyStream);
		logger.info("Reloaded Central PUMA PDP PDP");
		status = "OK";
	}

	@Override
	public String getCentralPUMAPolicy() {
		try {
			String str = FileUtils.readFileToString(new File(centralPUMAPolicyFilename));
			return str;
		} catch (IOException e) {
			logger.log(Level.WARNING, "IOException when reading Central PUMA PDP policy file", e);
			return "IOException";
		}
	}

}
