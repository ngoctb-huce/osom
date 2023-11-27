package org.etsi.osl.osom.serviceactions;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "fetchAndTerminateScheduledServices") // bean name
public class fetchAndTerminateScheduledServices implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchServiceQueueItems.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	public void execute(DelegateExecution execution) {
		logger.info("===================== fetchAndTerminateScheduledServices by Service Inventory Repository ====================");
		

		List<String> itemsToBeProcessed = serviceOrderManager.retrieveActiveServiceToTerminate();
		
		if ( itemsToBeProcessed != null ) {
			for (String serviceID : itemsToBeProcessed) {
				logger.info("Will TERMINATE service with id: " + serviceID );
				
				ServiceUpdate supd = new ServiceUpdate();
				supd.setState( ServiceStateType.TERMINATED );
				serviceOrderManager.updateService(serviceID, supd , true);
				
			}			
		}


	}

}
