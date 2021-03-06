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
package puma.central.pdp.tenant.attrservice;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.5.2
 * 2013-10-29T11:24:02.644+01:00
 * Generated source version: 2.5.2
 * 
 */
@WebServiceClient(name = "AttributeProviderService", 
                  wsdlLocation = "http://localhost:9001/attribute-service?wsdl",
                  targetNamespace = "http://attrservice.tenant.puma/") 
public class AttributeProviderService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://attrservice.tenant.puma/", "AttributeProviderService");
    public final static QName AttributeProviderPort = new QName("http://attrservice.tenant.puma/", "AttributeProviderPort");
    static {
        URL url = null;
        try {
            url = new URL("http://localhost:9001/attribute-service?wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(AttributeProviderService.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "http://localhost:9001/attribute-service?wsdl");
        }
        WSDL_LOCATION = url;
    }

    public AttributeProviderService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public AttributeProviderService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public AttributeProviderService() {
        super(WSDL_LOCATION, SERVICE);
    }
    

    /**
     *
     * @return
     *     returns AttributeProvider
     */
    @WebEndpoint(name = "AttributeProviderPort")
    public AttributeProvider getAttributeProviderPort() {
        return super.getPort(AttributeProviderPort, AttributeProvider.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns AttributeProvider
     */
    @WebEndpoint(name = "AttributeProviderPort")
    public AttributeProvider getAttributeProviderPort(WebServiceFeature... features) {
        return super.getPort(AttributeProviderPort, AttributeProvider.class, features);
    }

}
