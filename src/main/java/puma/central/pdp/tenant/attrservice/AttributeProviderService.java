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