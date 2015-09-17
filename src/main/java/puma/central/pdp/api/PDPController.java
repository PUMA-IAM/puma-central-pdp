package puma.central.pdp.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import puma.central.pdp.CentralPUMAPDP;
import puma.rest.domain.Identifiers;
import puma.rest.domain.Policy;
import puma.rest.domain.Request;
import puma.rest.domain.ResponseType;
import puma.rest.domain.Status;

@Controller
@RequestMapping(value = "/")
public class PDPController {

	@ResponseBody
	@RequestMapping(value = "/evaluate", method = RequestMethod.POST, produces="application/json", consumes="application/json")
	public ResponseType evaluate(@RequestBody Request request) {
		return CentralPUMAPDP.getInstance().evaluate(request.getAttributes());
	}

	@ResponseBody
	@RequestMapping(value = "/status", method = RequestMethod.GET, produces="application/json")
	public Status getStatus() {
		return new Status(CentralPUMAPDP.getInstance().getStatus());
	}

	@ResponseStatus(value = HttpStatus.OK)
	@RequestMapping(value = "/policy", method = RequestMethod.PUT, consumes="application/json")
	public void loadCentralPUMAPolicy(@RequestBody Policy policy) {
		CentralPUMAPDP.getInstance().loadCentralPUMAPolicy(policy.getPolicy());
	}

	@ResponseBody
	@RequestMapping(value = "/policy", method = RequestMethod.GET, produces="application/json")
	public Policy getCentralPUMAPolicy() {
		return new Policy(CentralPUMAPDP.getInstance().getCentralPUMAPolicy());
	}

	@ResponseStatus(value = HttpStatus.OK)
	@RequestMapping(value = "/{tenantIdentifier}/policy", method = RequestMethod.PUT, consumes="application/json")
	public void loadTenantPolicy(@PathVariable String tenantIdentifier,
			@RequestBody Policy policy) {
		CentralPUMAPDP.getInstance().loadTenantPolicy(tenantIdentifier, policy.getPolicy());
	}

	@ResponseBody
	@RequestMapping(value = "/ids", method = RequestMethod.GET, produces="application/json")
	public Identifiers getIdentifiers() {
		return new Identifiers(CentralPUMAPDP.getInstance().getIdentifiers());
	}

}
