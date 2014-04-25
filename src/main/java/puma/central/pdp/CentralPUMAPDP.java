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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import puma.central.pdp.util.PolicyAssembler;
import puma.peputils.thrift.PEPServer;
import puma.peputils.thrift.RemotePEPService;
import puma.rmi.pdp.CentralPUMAPDPRemote;
import puma.rmi.pdp.mgmt.CentralPUMAPDPMgmtRemote;
import puma.util.timing.TimerFactory;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
	
	private static final Boolean LOG_ENABLED = false;

	private static final int THRIFT_PORT = 9090;

	private static final String CENTRAL_PUMA_PDP_RMI_NAME = "central-puma-pdp";

	private static final String CENTRAL_PUMA_POLICY_FILENAME = "central-puma-policy.xml";
	
	private static final String GLOBAL_PUMA_POLICY_FILENAME = "global-puma-policy.xml";
	
	private static final String GLOBAL_PUMA_PDP_RMI_NAME = "global-puma-pdp";
	
	private static final String GLOBAL_PUMA_POLICY_ID = "global-puma-policy";

	private static final int RMI_REGISITRY_PORT = 2040;

	private static final Logger logger = Logger.getLogger(CentralPUMAPDP.class
			.getName());

	public static void main(String[] args) {
		// initialize log4j
		BasicConfigurator.configure();
		
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("ph", "policy-home", true,
				"The folder where to find the policy file given with the given policy id. "
				+ "For default operation, this folder should contain the central PUMA policy (called " + CENTRAL_PUMA_POLICY_FILENAME + ")");
		options.addOption("pid", "policy-id", true,
				"The id of the policy to be evaluated on decision requests. Default value: " + GLOBAL_PUMA_POLICY_ID + ")");
		options.addOption("s", "log-disabled", true, "Verbose mode (true/false)");
		String policyHome = "";
		String policyId = "";

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
			if (line.hasOption("policy-id")) {
				policyId = line.getOptionValue("policy-id");
			} else {
				logger.log(Level.INFO, "Using default policy id: " + GLOBAL_PUMA_POLICY_ID);
				policyId = GLOBAL_PUMA_POLICY_ID;
			}
			if (line.hasOption("log-disabled") && Boolean.parseBoolean(line.getOptionValue("log-disabled"))) {
				LogManager.getLogManager().reset();
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
		CentralPUMAPDP pdp;
		try {
			pdp = new CentralPUMAPDP(policyHome, policyId);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "FAILED to set up the CentralPUMAPDP. Quitting.", e);
			return;
		}

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
			CentralPUMAPDPRemote stub = (CentralPUMAPDPRemote) UnicastRemoteObject
					.exportObject(pdp, 0);
			registry.bind(CENTRAL_PUMA_PDP_RMI_NAME, stub);
			logger.info("Central PUMA PDP up and running (available using RMI with name \"central-puma-pdp\" on RMI registry port " + RMI_REGISITRY_PORT + ")");
			Thread.sleep(100); // MDC: vroeger eindigde de Thread om één of andere reden, dit lijkt te werken...
		} catch(Exception e) {
			logger.log(Level.SEVERE, "FAILED to set up PDP as RMI server", e);
		}
		
		//
		// STARTUP THE THRIFT SERVER
		//
		//logger.log(Level.INFO, "Not setting up the Thrift server");
		
		// set up server
		PEPServer handler = new PEPServer(new CentralPUMAPEP(pdp));
		RemotePEPService.Processor<PEPServer> processor = new RemotePEPService.Processor<PEPServer>(handler);
		TServerTransport serverTransport;
		try {
			serverTransport = new TServerSocket(THRIFT_PORT);
		} catch (TTransportException e) {
			e.printStackTrace();
			return;
		}
		TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));
		
		System.out.println("Setting up the Thrift server on port " + THRIFT_PORT);
		server.serve();
	}

	private MultiPolicyPDP pdp;

	public CentralPUMAPDP(String policyDir, String policyId) throws IOException {
		status = "NOT INITIALIZED";
		initializePDP(policyDir);
		this.policyId = policyId;
	}

	private String centralPUMAPolicyFilename;
	private String globalPUMAPolicyFilename;
	private String policyDir;	
	private String policyId; // the id of the policy to be evaluated for access requests
	
	private List<String> identifiers;	// List of identifiers that have at least one policy running on the pdp

	private String status;
	
	private static final String TIMER_NAME = "centralpumapdp.evaluate";

	/**
	 * Initialize the application PDP by scanning all policy files in the given
	 * directory.
	 * 
	 * This method should be called before the first call to isAuthorized().
	 * 
	 * @param policyDir
	 *            WITH trailing slash.
	 * @throws IOException 
	 */
	public void initializePDP(String policyDir) throws IOException {
		// Initialize some variables
		this.policyDir = policyDir;
		this.identifiers = new ArrayList<String>();
		// store for later usage
		this.centralPUMAPolicyFilename = policyDir
				+ CENTRAL_PUMA_POLICY_FILENAME;
		this.globalPUMAPolicyFilename = policyDir + GLOBAL_PUMA_POLICY_FILENAME;
		try {
			this.initializeGlobalPolicy();
			logger.info("Initialized global policy");
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Application policy file not found");
			status = "APPLICATION POLICY FILE NOT FOUND";
			throw e;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not initialize global policy", e);
			throw e;
		}
		// Generate the MultiPolicyPDP
		this.pdp = new MultiPolicyPDP(getPolicyStreams(policyDir));
		// Report success
		logger.info("initialized application PDP");
		status = "OK";
	}

	private void initializeGlobalPolicy() throws IOException {
		File destination = new File(this.globalPUMAPolicyFilename);
		destination.createNewFile();
		PolicyAssembler ass = new PolicyAssembler();
		PrintWriter writer = new PrintWriter(this.globalPUMAPolicyFilename, "UTF-8");
		writer.print(ass.assemble(detectDeployedTenantPolicies()));
		writer.close();
		
		/*File sourceFile = new File(this.centralPUMAPolicyFilename);
		File destFile = new File(this.globalPUMAPolicyFilename);
		
		if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if (source != null) {
	            source.close();
	        }
	        if (destination != null) {
	            destination.close();
	        }
	    }*/
	}

	private List<InputStream> getPolicyStreams(String policyDir) {
		File dir = new File(policyDir);
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

	private List<String> detectDeployedTenantPolicies() {
		List<String> result = new ArrayList<String>();
		File currentDirectory = new File(this.policyDir);
		for (File next: currentDirectory.listFiles()) {
			if(next.isFile() && !next.getName().endsWith("~")) {
				Long tenantIdentifier;
				try {
					tenantIdentifier = Long.parseLong(next.getName().substring(0, next.getName().indexOf(".")));
					this.registerPolicy(tenantIdentifier.toString());
					result.add("" + tenantIdentifier);
					logger.info("Detected tenant policy \"" + next.getName() + "\"");
				} catch (NumberFormatException ex) {
					continue;
				}
			}
		}
		return result;
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) throws RemoteException {
		Timer.Context timerCtx = TimerFactory.getInstance().getTimer(getClass(), TIMER_NAME).time();
		
		String log = "Received policy request for Central PUMA PDP. Cached attributes:\n";
		for(CachedAttribute a: cachedAttributes) {
			log += a.getId() + " = " + a.getValue().toString() + "\n";
		}
		logger.info(log);
		
		ResponseCtx response = this.pdp.evaluate(this.policyId, request,
				cachedAttributes);

		// print out some information
		String msg = "Results in response to request : ";
		for (Object r : response.getResults()) {
			Result result = (Result) r;
			msg += "(status: " + result.getStatus() + ", result: "
					+ result.getHumanReadableDecision() + ", #obligations: "
					+ result.getObligations().size() + ") ";
		}
		logger.info(msg);
		
		timerCtx.stop();
		
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
	public void loadTenantPolicy(String tenantIdentifier, String policy) {
		// Write the tenant policy
		try {
			PrintWriter writer = new PrintWriter(this.constructFilename(tenantIdentifier), "UTF-8");	
			writer.print(policy);
			writer.close();
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
		// Register the tenant
		this.registerPolicy(tenantIdentifier);
	}
	
	/**
	 * Registers a tenant into the PDP manager
	 * 
	 * Note: this method should ONLY be called if a file with the corresponding filename {@code constructFilename} has been created and written to
	 * @param tenantIdentifier
	 */
	private void registerPolicy(String tenantIdentifier) {		
		// Rewrite the central policy and make sure there is a reference to the added policy
		PolicyAssembler ass = null;
		PrintWriter writer;
		if (!this.identifiers.contains(tenantIdentifier))
			this.identifiers.add(tenantIdentifier);
		try {
			//stream = new FileOutputStream(this.globalPUMAPolicyFilename);
			writer = new PrintWriter(this.globalPUMAPolicyFilename, "UTF-8");
			ass = new PolicyAssembler();
			writer.print(ass.assemble(this.identifiers));
			writer.close();
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING, "Unable to deploy policy", e);
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.WARNING, "Unable to deploy policy", e);
		}
		// Finish
		logger.info("Succesfully deployed new tenant policy " + this.constructFilename(tenantIdentifier));
		this.reload();	// Reloads the pdp (including policy finder modules)
	}

	private String constructFilename(String tenantIdentifier) {
		return this.policyDir + tenantIdentifier + ".xml";
	}

	@Override
	public void reload() {
		// just set up a new PDP
		this.pdp = new MultiPolicyPDP(getPolicyStreams(policyDir));
		logger.info("Reloaded policies at central PUMA PDP");
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

	@Override
	public List<String> getIdentifiers() {
		return this.identifiers;
	}

	@Override
	public String getMetrics() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		try {
			return writer.writeValueAsString(TimerFactory.getInstance().getMetricRegistry());
		} catch (JsonProcessingException e) {
			logger.log(Level.WARNING, "Exception on JSON encoding of metrics", e);
			return "";
		}
	}

	@Override
	public void resetMetrics() throws RemoteException {
		TimerFactory.getInstance().resetAllTimers();
	}

}
