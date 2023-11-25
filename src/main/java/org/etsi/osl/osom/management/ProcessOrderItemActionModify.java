package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.sim638.model.Service;
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

@Component(value = "processOrderItemActionModify") // bean name
public class ProcessOrderItemActionModify implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ProcessOrderItemActionModify.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("ProcessOrderItemActionModify:" + execution.getVariableNames().toString());

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
		
		if ( soi.getState().equals(  ServiceOrderStateType.ACKNOWLEDGED    ) ) {
			
			
			if ( refservice.getState().equals(  ServiceStateType.INACTIVE) 
					||  refservice.getState().equals(  ServiceStateType.TERMINATED)) {
			
				
				for (ServiceRef sref : soi.getService().getSupportingService() ) {
					ServiceUpdate supd = new ServiceUpdate();
					supd.setState( ServiceStateType.TERMINATED );
					serviceOrderManager.updateService( sref.getId(), supd , true);
				}
			}		
			else {
				

				//na doume to modify (me change characteristics apo to service restriction kai to terminate)
				//copy characteristics values from Service restriction to supporting services.
				for (ServiceRef sref : soi.getService().getSupportingService() ) {
					Service aService = serviceOrderManager.retrieveService( sref.getId() );
					ServiceUpdate supd = new ServiceUpdate();
					
					if ( soi.getService().getServiceCharacteristic() != null ) {
						for (Characteristic serviceChar : aService.getServiceCharacteristic() ) {
							
							for (Characteristic soiCharacteristic : soi.getService().getServiceCharacteristic()) {
								if ( soiCharacteristic.getName().contains( aService.getName() + "::" +serviceChar.getName() )) { //copy only characteristics that are related from the order
									
									serviceChar.setValue( soiCharacteristic.getValue() );
									supd.addServiceCharacteristicItem( serviceChar );		
								}
							}
						}
						
															
					}
					

					serviceOrderManager.updateService( aService.getId(), supd , true); //update the service
				}
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
						
			serviceOrderUpd.addOrderItemItem(orderItemItem);
		}
		
		
		serviceOrderManager.updateServiceOrderOrder( sor.getId(), serviceOrderUpd );
	}

}
