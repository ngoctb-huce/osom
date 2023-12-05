package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderActionType;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;

@Component(value = "processOrderItemActionCheck") // bean name
public class ProcessOrderItemActionCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ProcessOrderItemActionCheck.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("ProcessOrderItemActionCheck:" + execution.getVariableNames().toString());
		ServiceOrder sor = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderid"));
		String orderItemIdToProcess = (String) execution.getVariable("orderItemId");
		ServiceOrderItem soi = null;
		
		for (ServiceOrderItem i : sor.getOrderItem()) {
			if (i.getUuid().equals( orderItemIdToProcess )){
				soi = i;
				break;
			}
		}
		
		if ( soi == null ) {
			return;
		}
		
		if ( soi.getAction().equals(  ServiceOrderActionType.ADD   ) ) {	
			execution.setVariable("saction", "ADD");			
		} else if ( soi.getAction().equals(  ServiceOrderActionType.MODIFY   ) ) {	
			execution.setVariable("saction", "MODIFY");						
		}else if ( soi.getAction().equals(  ServiceOrderActionType.DELETE   ) ) {	
			execution.setVariable("saction", "DELETE");						
		}
	}

}
