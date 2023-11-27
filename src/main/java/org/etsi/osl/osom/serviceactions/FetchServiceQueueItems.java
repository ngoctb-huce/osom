package org.etsi.osl.osom.serviceactions;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "fetchServiceQueueItems") // bean name
public class FetchServiceQueueItems implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchServiceQueueItems.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	public void execute(DelegateExecution execution) {
		logger.info("=====================  FetchServiceQueueItems by Service Inventory Repository ===================== ");
		

		List<ServiceActionQueueItem> itemsToBeProcessed = serviceOrderManager.retrieveServiceQueueItems();
		if ( itemsToBeProcessed!= null ) {
			for (ServiceActionQueueItem serviceActionQueueItem : itemsToBeProcessed) {
				logger.debug("FetchServiceQueueItems serviceActionQueueItem getServiceRefId = " + serviceActionQueueItem.getServiceRefId());
				
			}			
		} 
		
		
		List<String> itemListAsString = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			if ( itemsToBeProcessed!=null) {			
				for (ServiceActionQueueItem item : itemsToBeProcessed) {
					String o = mapper.writeValueAsString(item);
					itemListAsString.add(o);
				}	
			}

			execution.setVariable("serviceActionsToBeProcessed", itemListAsString);

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
