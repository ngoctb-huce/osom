package org.etsi.osl.osom.management;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "findOrderItems") // bean name
public class FindOrderItems  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FindOrderItems.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info("FindOrderItems: " + execution.getVariables().toString());
		//orderItemsToBeProcessed
		//orderItemId

		if (execution.getVariable("orderid") instanceof String) {
			logger.debug("Will find items of order with id = " + execution.getVariable("orderid"));
			ServiceOrder sor = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderid"));

			List<String> orderItemsToBeProcessed = new ArrayList<>();

			for (ServiceOrderItem soi : sor.getOrderItem()) {
				orderItemsToBeProcessed.add( soi.getUuid());				
			}

			execution.setVariable("orderItemsToBeProcessed", orderItemsToBeProcessed);
		}

	}
}
