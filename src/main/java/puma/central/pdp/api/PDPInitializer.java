package puma.central.pdp.api;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import puma.central.pdp.CentralPUMAPDP;

public class PDPInitializer implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		CentralPUMAPDP.initialize(System.getProperty("puma.centralpdp.policydir"));
		
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Do nothing?
		
	}

}
