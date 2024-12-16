package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "metricoOrchestrationService") //bean name
public class MetricoOrchestrationService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(NFVOrchestrationService.class.getName());


	@Value("${spring.application.name}")
	private String compname;
	
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;
	

    @Override
    public void execute(DelegateExecution execution) {
		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
		logger.info("Service name:" + aService.getName() );
		logger.info("Service state:" + aService.getState()  );			
		logger.info("Request to TMF628 for Service: " + aService.getId() );
		
		//we need to retrieve here the Service Spec of this service that we send to the NFVO
		
		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
		
		if ( spec!=null ) {			

			ServiceSpecCharacteristic c = spec.getServiceSpecCharacteristicByName( "NSDID" );						

		
		
    }

}
