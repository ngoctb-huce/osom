package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderActionType;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import org.etsi.osl.tmf.so641.model.ServiceRestriction;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "processOrderItemActionDelete") // bean name
public class ProcessOrderItemActionDelete implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ProcessOrderItemActionDelete.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("ProcessOrderItemActionDelete:" + execution.getVariableNames().toString());
		
		logger.debug("Will process/orchestrate order with id = " + execution.getVariable("orderid"));
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
		
		ServiceRestriction refservice = soi.getService();
		
		/**
		 * we will terminate the services
		 */
		if ( soi.getState().equals(  ServiceOrderStateType.ACKNOWLEDGED    ) ) {

			for (ServiceRef sref : soi.getService().getSupportingService() ) {
				ServiceUpdate supd = new ServiceUpdate();
				supd.setState( ServiceStateType.TERMINATED );
				serviceOrderManager.updateService( sref.getId(), supd , true);
			}
			
		}
		
		
		
		soi.setState(ServiceOrderStateType.INPROGRESS);
		soi.setAction( ServiceOrderActionType.NOCHANGE ); //reset the action to NOCHANGE	

		
		/***
		 * we can update now the serviceorder element in catalog
		 * Update also the related service attributes
		 */
		
		ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();
		for (ServiceOrderItem orderItemItem : sor.getOrderItem()) {
			orderItemItem.getService().setName( orderItemItem.getService().getServiceSpecification().getName() );
			orderItemItem.getService().setCategory( orderItemItem.getService().getServiceSpecification().getType() );
			//orderItemItem.getService().setState( ServiceStateType.RESERVED );
			//orderItemItem.setAction( ServiceOrderActionType.NOCHANGE ); //reset the action to NOCHANGE	
						
			serviceOrderUpd.addOrderItemItem(orderItemItem);
		}
		
		
		serviceOrderManager.updateServiceOrderOrder( sor.getId(), serviceOrderUpd );
	}

}
