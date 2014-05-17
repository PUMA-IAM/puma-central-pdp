package puma.central.pdp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import puma.rmi.pdp.CentralPUMAPDPRemote;
import puma.thrift.pdp.AttributeValueP;
import puma.thrift.pdp.DataTypeP;
import puma.thrift.pdp.RemotePDPService;
import puma.thrift.pdp.ResponseTypeP;

import com.sun.xacml.PDP;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.CachedAttribute;
import com.sun.xacml.ctx.EncodedCachedAttribute;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

public class RemotePDPTest {

	int NB_RUNS = 1;

	/**
	 * Our logger
	 */
	private final Logger logger = Logger.getLogger(PDP.class.getName());

	private static final String CENTRAL_PUMA_PDP_HOST = "puma-central-puma-pdp";

	private static final String CENTRAL_PUMA_PDP_RMI_NAME = "central-puma-pdp";

	private static final int CENTRAL_PUMA_PDP_RMI_REGISITRY_PORT = 2040;

	public static void main(String[] args) {
		RemotePDPTest test = new RemotePDPTest();
		test.open();
		test.test();
		test.close();
	}

	List<CachedAttribute> cachedAttributes;

	RemotePDPService.Client client;

	TTransport transport;

	private CentralPUMAPDPRemote centralPUMAPDP;

	public RemotePDPTest() {
		cachedAttributes = new LinkedList<CachedAttribute>();
		List<StringAttribute> stringAttributes = new LinkedList<StringAttribute>();
		stringAttributes.add(new StringAttribute("a"));
		stringAttributes.add(new StringAttribute("b"));
		stringAttributes.add(new StringAttribute("c"));
		cachedAttributes.add(new CachedAttribute(StringAttribute.identifier,
				"string-attribute", new BagAttribute(
						StringAttribute.identifierURI, stringAttributes)));
		List<StringAttribute> subjectId = new LinkedList<StringAttribute>();
		subjectId.add(new StringAttribute("1"));
		cachedAttributes.add(new CachedAttribute(StringAttribute.identifier,
				"subject:id", new BagAttribute(
						StringAttribute.identifierURI, subjectId)));
		List<IntegerAttribute> integerAttributes = new LinkedList<IntegerAttribute>();
		integerAttributes.add(new IntegerAttribute(1));
		integerAttributes.add(new IntegerAttribute(2));
		integerAttributes.add(new IntegerAttribute(3));
		cachedAttributes.add(new CachedAttribute(IntegerAttribute.identifier,
				"integer-attribute", new BagAttribute(
						IntegerAttribute.identifierURI, integerAttributes)));
		List<BooleanAttribute> booleanAttributes = new LinkedList<BooleanAttribute>();
		booleanAttributes.add(BooleanAttribute.getFalseInstance());
		cachedAttributes.add(new CachedAttribute(BooleanAttribute.identifier,
				"boolean-attribute", new BagAttribute(
						BooleanAttribute.identifierURI, booleanAttributes)));
		List<DateTimeAttribute> dateTimeAttributes = new LinkedList<DateTimeAttribute>();
		dateTimeAttributes.add(new DateTimeAttribute(new Date()));
		cachedAttributes.add(new CachedAttribute(DateTimeAttribute.identifier,
				"datetime-attribute", new BagAttribute(
						DateTimeAttribute.identifierURI, dateTimeAttributes)));
	}

	public void open() {
		// set up Thrift
		transport = new TSocket("puma-central", 9091);
		try {
			transport.open();
		} catch (TTransportException e) {
			e.printStackTrace();
		}

		TProtocol protocol = new TBinaryProtocol(transport);
		client = new RemotePDPService.Client(protocol);
		logger.info("Set up Thrift client");

		// set up RMI client
		try {
			Registry registry = LocateRegistry.getRegistry(
					CENTRAL_PUMA_PDP_HOST, CENTRAL_PUMA_PDP_RMI_REGISITRY_PORT);
			centralPUMAPDP = (CentralPUMAPDPRemote) registry
					.lookup(CENTRAL_PUMA_PDP_RMI_NAME);
		} catch (Exception e) {
			logger.log(Level.WARNING, "FAILED to reach the central PUMA PDP", e);
			centralPUMAPDP = null; // just to be sure
		}
		logger.info("Set up RMI client");
	}

	public void close() {
		transport.close();
	}

	public void test() {
		try {
//			System.out.println("Press enter to start the RMI tests");
//			System.in.read();
			System.out.println("Starting the RMI tests");
			double rmiDuration = 0;
			for (int i = 0; i < NB_RUNS; i++) {
				rmiDuration += testRMI();
			}
//			System.out.println("Press enter to start the Thrift tests");
//			System.in.read();
			System.out.println("Starting the Thrift tests");
			double thriftDuration = 0;
			for (int i = 0; i < NB_RUNS; i++) {
				thriftDuration += testThrift();
			}
			System.out.println("" + NB_RUNS + " runs: ");
			System.out.println("RMI: " + rmiDuration + "ms ( = " + (rmiDuration / NB_RUNS) + "ms per run)");
			System.out.println("Thrift: " + thriftDuration + "ms ( = " + (thriftDuration / NB_RUNS) + "ms per run)");
		} catch (TException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Duration in ms
	 * @throws TException
	 */
	public double testThrift() throws TException {
		double startTime = System.nanoTime();

		// preprocess the input
		List<AttributeValueP> values = new LinkedList<AttributeValueP>();
		for (CachedAttribute ca : cachedAttributes) {
			String type = ca.getType();
			if (type.equals(StringAttribute.identifier)) {
				AttributeValueP avp = new AttributeValueP(DataTypeP.STRING,
						ca.getId());
				for (StringAttribute av : (Collection<StringAttribute>) ca
						.getValue().getValue()) {
					avp.addToStringValues(av.getValue());
				}
				values.add(avp);
			} else if (type.equals(IntegerAttribute.identifier)) {
				AttributeValueP avp = new AttributeValueP(DataTypeP.INTEGER,
						ca.getId());
				for (IntegerAttribute av : (Collection<IntegerAttribute>) ca
						.getValue().getValue()) {
					avp.addToIntValues((int) av.getValue());
				}
				values.add(avp);
			} else if (type.equals(BooleanAttribute.identifier)) {
				AttributeValueP avp = new AttributeValueP(DataTypeP.BOOLEAN,
						ca.getId());
				for (BooleanAttribute av : (Collection<BooleanAttribute>) ca
						.getValue().getValue()) {
					avp.addToBooleanValues(av.getValue());
				}
				values.add(avp);
			} else if (type.equals(DateTimeAttribute.identifier)) {
				AttributeValueP avp = new AttributeValueP(DataTypeP.DATETIME,
						ca.getId());
				for (DateTimeAttribute av : (Collection<DateTimeAttribute>) ca
						.getValue().getValue()) {
					// NOTE: we store the time as the long resulting from
					// getTime()
					// This long is the number of milliseconds since 1970.
					// Also note that UNIX time is the number of *seconds* since
					// 1970.
					avp.addToDatetimeValues(av.getValue().getTime());
				}
				values.add(avp);
			} else {
				throw new RuntimeException("Unsupport attribute type given: "
						+ type);
			}
		}
		// do the request
		ResponseTypeP response = client.evaluateP(values);
		// test the response just to be sure
		if (response != ResponseTypeP.INDETERMINATE) {
			System.out.println("Incorrect evaluation result received: "
					+ response);
		}

		double endTime = System.nanoTime();
		return (endTime - startTime) / 1000000;
	}

	/**
	 * @return Duration in ms
	 * @throws TException
	 * @throws RemoteException 
	 */
	public double testRMI() throws TException, RemoteException {
		double startTime = System.nanoTime();

		// preprocess the input
		List<EncodedCachedAttribute> encodedCachedAttributes = new ArrayList<EncodedCachedAttribute>();
		for (CachedAttribute ca : cachedAttributes) {
			encodedCachedAttributes.add(new EncodedCachedAttribute(ca));
		}
		// do the request
		ResponseCtx response = centralPUMAPDP.evaluate(encodedCachedAttributes);
		// test the response just to be sure
		if (response.getResults().size() > 1) {
			logger.severe("More than one result in the Thrift PDP server? Nb results: "
					+ response.getResults().size() + " Returning Indeterminate");
		}
		if (response.getResults().size() == 0) {
			logger.severe("No results in the Thrift PDP server? Nb results: "
					+ response.getResults().size() + " Returning Indeterminate");
		}
		for (Object result : response.getResults()) {
			// there is only one result, just return on the first one
			Result r = (Result) result;
			if (r.getDecision() != Result.DECISION_INDETERMINATE) {
				logger.severe("Result was not INDETERMINATE? Result was: " + r.getDecision());
			}
		}

		double endTime = System.nanoTime();
		return (endTime - startTime) / 1000000;
	}
}
