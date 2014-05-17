package puma.central.pdp;

import java.util.List;

import org.apache.thrift.TException;

import puma.thrift.pdp.AttributeValueP;
import puma.thrift.pdp.RemotePDPService.Iface;
import puma.thrift.pdp.ResponseTypeP;

/**
 * Small helper class which allows to switch the PDP served by Thrift.
 * 
 * @author maartend
 *
 */
public class ThriftPDPServer implements Iface {
	
	private CentralPUMAPDP pdp;
	
	public ThriftPDPServer(CentralPUMAPDP pdp) {
		this.pdp = pdp;
	}
	
	public void setPDP(CentralPUMAPDP pdp) {
		this.pdp = pdp;
	}

	@Override
	public ResponseTypeP evaluateP(List<AttributeValueP> attributes)
			throws TException {
		return pdp.evaluateP(attributes);
	}

}
