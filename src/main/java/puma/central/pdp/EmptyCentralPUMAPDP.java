package puma.central.pdp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import org.apache.log4j.BasicConfigurator;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import puma.rmi.pdp.CentralPUMAPDPRemote;
import puma.thrift.pdp.AttributeValueP;
import puma.thrift.pdp.DataTypeP;
import puma.thrift.pdp.RemotePDPService;
import puma.thrift.pdp.ResponseTypeP;

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
 * An instance of the CentralPUMAPDP to test the network overhead. The evaluate() methods
 * for Thrift and RMI just return INDETERMINATE without evaluating a policy but with all the deserialziation and serialization
 * as would normal be the case.
 * 
 * @author Maarten Decat
 * 
 */
public class EmptyCentralPUMAPDP extends CentralPUMAPDP {

	private static final int THRIFT_PEP_PORT = 9090;

	private static final int THRIFT_PDP_PORT = 9091;

	private static final String CENTRAL_PUMA_PDP_RMI_NAME = "central-puma-pdp";

	private static final String CENTRAL_PUMA_POLICY_FILENAME = "central-puma-policy.xml";

	private static final String GLOBAL_PUMA_POLICY_FILENAME = "global-puma-policy.xml";

	private static final String GLOBAL_PUMA_POLICY_ID = "global-puma-policy";

	private static final int RMI_REGISITRY_PORT = 2040;

	private static final Logger logger = Logger.getLogger(EmptyCentralPUMAPDP.class
			.getName());

	public EmptyCentralPUMAPDP(String policyHome, String policyId) throws IOException {
		super(policyHome, policyId);
	}

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
		EmptyCentralPUMAPDP pdp;
		try {
			pdp = new EmptyCentralPUMAPDP(policyHome, policyId);
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
		RemotePDPService.Processor<EmptyCentralPUMAPDP> pdpProcessor = new RemotePDPService.Processor<EmptyCentralPUMAPDP>(pdp);
		TServerTransport pdpServerTransport;
		try {
			pdpServerTransport = new TServerSocket(THRIFT_PDP_PORT);
		} catch (TTransportException e) {
			e.printStackTrace();
			return;
		}
		TServer pdpServer = new TSimpleServer(new TServer.Args(pdpServerTransport).processor(pdpProcessor));
		System.out.println("Setting up the Thrift PDP server on port " + THRIFT_PDP_PORT);
		pdpServer.serve();
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(
			List<EncodedCachedAttribute> encodedCachedAttributes) {		
		// decode as we would normally do
		List<CachedAttribute> cachedAttributes = new LinkedList<CachedAttribute>();
		for (EncodedCachedAttribute eca : encodedCachedAttributes) {
			cachedAttributes.add(eca.toCachedAttribute());
		}
		logAttributes(cachedAttributes);
		
		return new ResponseCtx(new Result(Result.DECISION_INDETERMINATE));
	}

	/**
	 * Evaluate a request and return the result.
	 */
	public ResponseCtx evaluate(RequestType request,
			List<CachedAttribute> cachedAttributes) {		
		logAttributes(cachedAttributes);
		return new ResponseCtx(new Result(Result.DECISION_INDETERMINATE));
	}

	/********************************
	 * FOR THE THRIFT PDP SERVER
	 */

	@Override
	public ResponseTypeP evaluateP(List<AttributeValueP> attributes)
			throws TException {
		// decode as we would normally do
		List<CachedAttribute> cachedAttributes = new LinkedList<CachedAttribute>();
		for(AttributeValueP avp: attributes) {
			cachedAttributes.add(convert(avp));
		}
		
		logAttributes(cachedAttributes);
		
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
	
	public void logAttributes(List<CachedAttribute> cachedAttributes) {
		String toLog = "";
		for(CachedAttribute ca: cachedAttributes) {
			toLog += ca.toString() + "\n";
		}
		logger.info("Received new PDP evaluation request:\n" + toLog);
	}
}
