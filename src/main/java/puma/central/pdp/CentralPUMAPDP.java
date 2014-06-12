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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import oasis.names.tc.xacml._2_0.context.schema.os.ActionType;
import oasis.names.tc.xacml._2_0.context.schema.os.AttributeType;
import oasis.names.tc.xacml._2_0.context.schema.os.AttributeValueType;
import oasis.names.tc.xacml._2_0.context.schema.os.EnvironmentType;
import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;
import oasis.names.tc.xacml._2_0.context.schema.os.ResourceType;
import oasis.names.tc.xacml._2_0.context.schema.os.SubjectType;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import puma.central.pdp.util.PolicyAssembler;
import puma.rmi.pdp.CentralPUMAPDPRemote;
import puma.rmi.pdp.mgmt.CentralPUMAPDPMgmtRemote;
import puma.thrift.pdp.AttributeValueP;
import puma.thrift.pdp.DataTypeP;
import puma.thrift.pdp.RemotePDPService;
import puma.thrift.pdp.ResponseTypeP;
import puma.util.timing.TimerFactory;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.CachedAttribute;
import com.sun.xacml.ctx.EncodedCachedAttribute;
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
public class CentralPUMAPDP implements CentralPUMAPDPRemote,
		CentralPUMAPDPMgmtRemote, RemotePDPService.Iface {

	private static final int THRIFT_PEP_PORT = 9090;

	private static final int THRIFT_PDP_PORT = 9091;

	private static final String CENTRAL_PUMA_PDP_RMI_NAME = "central-puma-pdp";

	private static final String CENTRAL_PUMA_POLICY_FILENAME = "central-puma-policy.xml";

	private static final String GLOBAL_PUMA_POLICY_FILENAME = "global-puma-policy.xml";

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
			if (line.hasOption("log-disabled") && Boolean.parseBoolean(line.getOptionValue("log-disabled"))) {
				logger.log(Level.INFO, "Now switching to silent mode");
				LogManager.getLogManager().getLogger("").setLevel(Level.WARNING);
				//LogManager.getLogManager().reset();
			} 
			if (line.hasOption("policy-id")) {
				policyId = line.getOptionValue("policy-id");
			} else {
				logger.log(Level.INFO, "Using default policy id: " + GLOBAL_PUMA_POLICY_ID);
				policyId = GLOBAL_PUMA_POLICY_ID;
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
		final CentralPUMAPDP pdp;
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
		// STARTUP THE THRIFT PEP SERVER
		//
		//logger.log(Level.INFO, "Not setting up the Thrift server");
		
		// set up server
//		PEPServer handler = new PEPServer(new CentralPUMAPEP(pdp));
//		RemotePEPService.Processor<PEPServer> processor = new RemotePEPService.Processor<PEPServer>(handler);
//		TServerTransport serverTransport;
//		try {
//			serverTransport = new TServerSocket(THRIFT_PEP_PORT);
//		} catch (TTransportException e) {
//			e.printStackTrace();
//			return;
//		}
//		TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));
//		System.out.println("Setting up the Thrift PEP server on port " + THRIFT_PEP_PORT);
//		server.serve();
		//
		// STARTUP THE THRIFT PEP SERVER
		//
		//logger.log(Level.INFO, "Not setting up the Thrift server");
		
		// set up server
		// do this in another thread not to block the main thread
		new Thread(new Runnable() {			
			@Override
			public void run() {
				RemotePDPService.Processor<CentralPUMAPDP> pdpProcessor = new RemotePDPService.Processor<CentralPUMAPDP>(pdp);
				TServerTransport pdpServerTransport;
				try {
					pdpServerTransport = new TServerSocket(THRIFT_PDP_PORT);
				} catch (TTransportException e) {
					e.printStackTrace();
					return;
				}
				TServer pdpServer = new TThreadPoolServer(new TThreadPoolServer.Args(pdpServerTransport).processor(pdpProcessor));
				logger.info("Setting up the Thrift PDP server on port " + THRIFT_PDP_PORT);
				pdpServer.serve();
			}
		}).start();
	}

	private MultiPolicyPDP pdp;

	public CentralPUMAPDP(String policyDir) throws IOException {
		this(policyDir, null);
	}

	public CentralPUMAPDP(String policyDir, String policyId) throws IOException {
		status = "NOT INITIALIZED";
		initializePDP(policyDir);
		this.policyId = policyId;
	}

	private String centralPUMAPolicyFilename;
	private String globalPUMAPolicyFilename;
	private String policyDir;
	private String policyId; // the id of the policy to be evaluated for access
								// requests

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}

	private List<String> identifiers; // List of identifiers that have at least
										// one policy running on the pdp

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
		logger.info("initialized Central PDP");
		status = "OK";
	}

	private void initializeGlobalPolicy() throws IOException {
		File destination = new File(this.globalPUMAPolicyFilename);
		destination.createNewFile();
		PolicyAssembler ass = new PolicyAssembler();
		PrintWriter writer = new PrintWriter(this.globalPUMAPolicyFilename,
				"UTF-8");
		writer.print(ass.assemble(detectDeployedTenantPolicies()));
		writer.close();

		/*
		 * File sourceFile = new File(this.centralPUMAPolicyFilename); File
		 * destFile = new File(this.globalPUMAPolicyFilename);
		 * 
		 * if(!destFile.exists()) { destFile.createNewFile(); }
		 * 
		 * FileChannel source = null; FileChannel destination = null;
		 * 
		 * try { source = new FileInputStream(sourceFile).getChannel();
		 * destination = new FileOutputStream(destFile).getChannel();
		 * destination.transferFrom(source, 0, source.size()); } finally { if
		 * (source != null) { source.close(); } if (destination != null) {
		 * destination.close(); } }
		 */
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
		for (File next : currentDirectory.listFiles()) {
			if (next.isFile() && !next.getName().endsWith("~")) {
				Long tenantIdentifier;
				try {
					tenantIdentifier = Long.parseLong(next.getName().substring(
							0, next.getName().indexOf(".")));
					this.registerPolicy(tenantIdentifier.toString());
					result.add("" + tenantIdentifier);
					logger.info("Detected tenant policy \"" + next.getName()
							+ "\"");
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
	public ResponseCtx evaluate(
			List<EncodedCachedAttribute> encodedCachedAttributes) {
		Timer.Context timerCtx = TimerFactory.getInstance()
				.getTimer(getClass(), TIMER_NAME).time();

		List<CachedAttribute> cachedAttributes = new LinkedList<CachedAttribute>();
		for (EncodedCachedAttribute eca : encodedCachedAttributes) {
			cachedAttributes.add(eca.toCachedAttribute());
		}
		logAttributes(cachedAttributes, "RMI (new encoded API)");

		ResponseCtx response = _evaluate(null, cachedAttributes);

		timerCtx.stop();

		return response;
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) {		
		Timer.Context timerCtx = TimerFactory.getInstance()
				.getTimer(getClass(), TIMER_NAME).time();
		logAttributes(cachedAttributes, "RMI (comlete API)");
		ResponseCtx response = _evaluate(request, cachedAttributes);
		timerCtx.stop();
		return response;
	}
	
	private ResponseCtx _evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) {		
		if (request == null) {
			request = defaultRequest();
		}

		if (isLoggingAll()) {
			String log = "Received policy request for Central PUMA PDP. Cached attributes:\n";
			for (CachedAttribute a : cachedAttributes) {
				log += a.getId() + " = " + a.getValue().toString() + "\n";
			}
			logger.info(log);
		}

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
		
		return response;
	}

	private RequestType defaultRequest() {
		SubjectType xacmlSubject = new SubjectType();
		AttributeType subjectId = new AttributeType();
		subjectId.setAttributeId("subject:id-which-should-never-be-needed");
		subjectId.setDataType(StringAttribute.identifier);
		AttributeValueType subjectIdValue = new AttributeValueType();
		// subjectIdValue.getContent().add(subject.getId());
		subjectIdValue.getContent().add(
				"THE-SUBJECT-ID-IN-THE-REQUEST-WHICH-SHOULD-NEVER-BE-NEEDED");
		subjectId.getAttributeValue().add(subjectIdValue);
		xacmlSubject.getAttribute().add(subjectId);

		ResourceType xacmlObject = new ResourceType();
		AttributeType objectId = new AttributeType();
		objectId.setAttributeId(EvaluationCtx.RESOURCE_ID); // this should be
															// the official id
															// apparently
		objectId.setDataType(StringAttribute.identifier);
		AttributeValueType objectIdValue = new AttributeValueType();
		// objectIdValue.getContent().add(object.getId());
		objectIdValue.getContent().add(
				"THE-OBJECT-ID-IN-THE-REQUEST-WHICH-SHOULD-NEVER-BE-NEEDED");
		objectId.getAttributeValue().add(objectIdValue);
		xacmlObject.getAttribute().add(objectId);

		ActionType xacmlAction = new ActionType();
		AttributeType actionId = new AttributeType();
		actionId.setAttributeId("action:id-which-should-never-be-needed");
		actionId.setDataType(StringAttribute.identifier);
		AttributeValueType actionIdValue = new AttributeValueType();
		// actionIdValue.getContent().add(action.getId());
		actionIdValue.getContent().add(
				"THE-ACTION-ID-IN-THE-REQUEST-WHICH-SHOULD-NEVER-BE-NEEDED");
		actionId.getAttributeValue().add(actionIdValue);
		xacmlAction.getAttribute().add(actionId);

		EnvironmentType xacmlEnvironment = new EnvironmentType(); // empty in
																	// the
																	// request

		RequestType xacmlRequest = new RequestType();
		xacmlRequest.getSubject().add(xacmlSubject);
		xacmlRequest.getResource().add(xacmlObject);
		xacmlRequest.setAction(xacmlAction);
		xacmlRequest.setEnvironment(xacmlEnvironment);

		return xacmlRequest;
	}

	private Boolean isLoggingAll() {
		return !LogManager.getLogManager().getLogger("").getLevel()
				.equals(Level.WARNING);
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
			logger.log(
					Level.SEVERE,
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
			PrintWriter writer = new PrintWriter(
					this.constructFilename(tenantIdentifier), "UTF-8");
			writer.print(policy);
			writer.close();
		} catch (FileNotFoundException e) {
			logger.log(
					Level.SEVERE,
					"Application policy file not found when writing new Central PUMA PDP policy",
					e);
			return;
		} catch (UnsupportedEncodingException e) {
			logger.log(
					Level.SEVERE,
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
	 * Note: this method should ONLY be called if a file with the corresponding
	 * filename {@code constructFilename} has been created and written to
	 * 
	 * @param tenantIdentifier
	 */
	private void registerPolicy(String tenantIdentifier) {
		// Rewrite the central policy and make sure there is a reference to the
		// added policy
		PolicyAssembler ass = null;
		PrintWriter writer;
		if (!this.identifiers.contains(tenantIdentifier))
			this.identifiers.add(tenantIdentifier);
		try {
			// stream = new FileOutputStream(this.globalPUMAPolicyFilename);
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
		logger.info("Succesfully deployed new tenant policy "
				+ this.constructFilename(tenantIdentifier));
		this.reload(); // Reloads the pdp (including policy finder modules)
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
			String str = FileUtils.readFileToString(new File(
					centralPUMAPolicyFilename));
			return str;
		} catch (IOException e) {
			logger.log(Level.WARNING,
					"IOException when reading Central PUMA PDP policy file", e);
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
			return writer.writeValueAsString(TimerFactory.getInstance()
					.getMetricRegistry());
		} catch (JsonProcessingException e) {
			logger.log(Level.WARNING, "Exception on JSON encoding of metrics",
					e);
			return "";
		}
	}

	GraphiteReporter reporter = null;

	@Override
	public void resetMetrics() throws RemoteException {
		TimerFactory.getInstance().resetAllTimers();

		// connect metrics to the Graphite server
		if (reporter != null) {
			reporter.stop();
		}
		final Graphite graphite = new Graphite(new InetSocketAddress(
				"172.16.4.2", 2003));
		reporter = GraphiteReporter
				.forRegistry(TimerFactory.getInstance().getMetricRegistry())
				.prefixedWith("puma-central-pdp")
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.filter(MetricFilter.ALL).build(graphite);
		reporter.start(10, TimeUnit.SECONDS);
	}

	@Override
	public boolean ping() throws RemoteException {
		return true;
	}

	/********************************
	 * FOR THE THRIFT PDP SERVER
	 */

	@Override
	public ResponseTypeP evaluateP(List<AttributeValueP> attributes)
			throws TException {
		Timer.Context timerCtx = TimerFactory.getInstance()
				.getTimer(getClass(), TIMER_NAME).time();
		List<CachedAttribute> cachedAttributes = new LinkedList<CachedAttribute>();
		for(AttributeValueP avp: attributes) {
			cachedAttributes.add(convert(avp));
		}
		
		logAttributes(cachedAttributes, "Thrift");
		
		ResponseCtx response = this._evaluate(null, cachedAttributes);
		timerCtx.stop();
		// FIXME incomplete implementation here
		if(response.getResults().size() > 1) {
			logger.severe("More than one result in the Thrift PDP server? Nb results: " + response.getResults().size() + " Returning Indeterminate");
			return ResponseTypeP.INDETERMINATE;
		} 
		if(response.getResults().size() == 0) {
			logger.severe("No results in the Thrift PDP server? Nb results: " + response.getResults().size() + " Returning Indeterminate");
			return ResponseTypeP.INDETERMINATE;
		} 
		for(Object result: response.getResults()) {
			// there is only one result, just return on the first one
			Result r = (Result) result;
			if(r.getDecision() == Result.DECISION_DENY) {
				return ResponseTypeP.DENY;
			} else if(r.getDecision() == Result.DECISION_PERMIT) {
				return ResponseTypeP.PERMIT;
			} else if(r.getDecision() == Result.DECISION_NOT_APPLICABLE) {
				return ResponseTypeP.NOT_APPLICABLE;
			} else {
				return ResponseTypeP.INDETERMINATE;
			} 
		}
		// we should never end up here
		return ResponseTypeP.INDETERMINATE;
		
	}

	private CachedAttribute convert(AttributeValueP attribute) {
		if (attribute.getDataType() == DataTypeP.STRING) {
			List<AttributeValue> values = new ArrayList<AttributeValue>();
			for (String s : attribute.getStringValues()) {
				values.add(new StringAttribute(s));
			}
			return new CachedAttribute(StringAttribute.identifier,
					attribute.getId(), new BagAttribute(
							StringAttribute.identifierURI, values));
		} else if (attribute.getDataType() == DataTypeP.INTEGER) {
			List<AttributeValue> values = new ArrayList<AttributeValue>();
			for (Integer i : attribute.getIntValues()) {
				values.add(new IntegerAttribute(i));
			}
			return new CachedAttribute(IntegerAttribute.identifier,
					attribute.getId(), new BagAttribute(
							IntegerAttribute.identifierURI, values));
		} else if (attribute.getDataType() == DataTypeP.BOOLEAN) {
			List<AttributeValue> values = new ArrayList<AttributeValue>();
			for (Boolean b : attribute.getBooleanValues()) {
				values.add(BooleanAttribute.getInstance(b));
			}
			return new CachedAttribute(BooleanAttribute.identifier,
					attribute.getId(), new BagAttribute(
							BooleanAttribute.identifierURI, values));
		} else if (attribute.getDataType() == DataTypeP.DATETIME) {
			List<AttributeValue> values = new ArrayList<AttributeValue>();
			for (Long l : attribute.getDatetimeValues()) {
				values.add(new DateTimeAttribute(new Date(l)));
			}
			return new CachedAttribute(DateTimeAttribute.identifier,
					attribute.getId(), new BagAttribute(
							DateTimeAttribute.identifierURI, values));
		} else {
			throw new RuntimeException("Unsupported attribute type: "
					+ attribute.getDataType());
		}
	}
	
	public void logAttributes(List<CachedAttribute> cachedAttributes, String label) {
		String toLog = "";
		for(CachedAttribute ca: cachedAttributes) {
			toLog += ca.toString() + "\n";
		}
		logger.info("Received new " + label + " PDP evaluation request:\n" + toLog);
	}
}
