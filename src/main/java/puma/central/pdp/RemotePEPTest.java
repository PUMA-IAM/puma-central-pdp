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
import puma.peputils.Action;
import puma.peputils.Environment;
import puma.peputils.PEP;
import puma.peputils.Subject;
import puma.peputils.attributes.EnvironmentAttributeValue;
import puma.peputils.attributes.Multiplicity;
import puma.peputils.attributes.ObjectAttributeValue;
import puma.peputils.attributes.SubjectAttributeValue;
import puma.peputils.thrift.RemotePEP;


public class RemotePEPTest {

	public static void main(String[] args) {
		// 0. The PDP is already initialized by the PDPInitializer 
		
		// 1. First build your subject, object, action and environment, for example
		// based on the current Session or some parameters in the request
		Subject subject = new Subject("maarten");
		SubjectAttributeValue roles = new SubjectAttributeValue("roles", Multiplicity.GROUPED);
		roles.addValue("phd");
		roles.addValue("imindsr");
		subject.addAttributeValue(roles);
		subject.addAttributeValue(new SubjectAttributeValue("departement", Multiplicity.ATOMIC, "computer-science"));
		subject.addAttributeValue(new SubjectAttributeValue("fired", Multiplicity.ATOMIC, false));
		subject.addAttributeValue(new SubjectAttributeValue("tenant", Multiplicity.GROUPED, "KUL"));
		subject.addAttributeValue(new SubjectAttributeValue("email", Multiplicity.ATOMIC, "maarten.decat@cs.kuleuven.be"));
		
		puma.peputils.Object object = new puma.peputils.Object("123"); // damn, Object moet blijkbaar niet geïmporteerd worden...
		object.addAttributeValue(new ObjectAttributeValue("type", Multiplicity.ATOMIC, "document"));
		object.addAttributeValue(new ObjectAttributeValue("owning-tenant", Multiplicity.ATOMIC, "KUL"));
		object.addAttributeValue(new ObjectAttributeValue("location", Multiplicity.ATOMIC, "/docs/stuff/blabla/123.pdf"));
		object.addAttributeValue(new ObjectAttributeValue("sender", Multiplicity.ATOMIC, "bert"));
		ObjectAttributeValue destinations = new ObjectAttributeValue("destinations", Multiplicity.GROUPED);
		destinations.addValue("distrinet@cs.kuleuven.be");
		destinations.addValue("iemand@example.com");
		
		Action action = new Action("read");
		
		Environment environment = new Environment();
		environment.addAttributeValue(new EnvironmentAttributeValue("system-status", Multiplicity.ATOMIC, "overload"));
		environment.addAttributeValue(new EnvironmentAttributeValue("system-load", Multiplicity.ATOMIC, 90));
		
		// 2. Then just ask the PEP for a decision
		PEP pep = new RemotePEP("puma-simac", 9090);
		boolean authorized = pep.isAuthorized(subject, object, action, environment);
		
		// 3. Enforce the decision
		if(!authorized) {
			System.out.println("You shall not pass.");
			return;
		}
		
		System.out.println("You are authorized, here you can see the contents of document #123");
	}

}
