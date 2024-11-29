/*-
 * ========================LICENSE_START=================================
 * org.etsi.osl.osom
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.etsi.osl.osom.management;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.po622.model.ProductOrder;
import org.etsi.osl.tmf.po622.model.ProductOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "fetchAcknowledgedProductOrders") // bean name
public class FetchAcknowledgedProductOrders implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchAcknowledgedProductOrders.class.getName());


    @Autowired
    private ProductOrderManager productOrderManager;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	private TaskService taskService;

	
	public void execute(DelegateExecution execution) {
		logger.info("======================" + execution.getProcessDefinitionId()  + "======================================");
		logger.info("FetchAcknowledgedProductOrders by Product Order Repository");

		List<String> ordersToBeProcessed = null;
		if (execution.getVariable("productOrdersToBeProcessed") instanceof ArrayList) {
			ordersToBeProcessed = (ArrayList<String>) execution.getVariable("productOrdersToBeProcessed");
			for (String orderid : ordersToBeProcessed) {
				logger.info("productOrdersToBeProcessed From Previous = " + orderid);
			}
		} else {
			ordersToBeProcessed = new ArrayList<>();
		}

		List<String> orderlist = productOrderManager.retrieveOrdersByState( ProductOrderStateType.ACKNOWLEDGED );
		
		if ( orderlist != null ) {
			for (String orderid : orderlist) {
				if ( !ordersToBeProcessed.contains( orderid )  ) {
					

					ProductOrder sor = productOrderManager.retrieveProductOrder( orderid );
					if ( sor !=null && sor.getRequestedStartDate() != null ) {
						Instant instant = Instant.now() ;                          // Capture the current moment as seen in UTC.
						boolean canStart = sor.getRequestedStartDate().toInstant().isBefore( instant ) ;
						
						if ( canStart ) {
							logger.info("Product order is scheduled to start now, orderid= "+ orderid +" date="+  sor.getRequestedStartDate().toInstant());
							ordersToBeProcessed.add( orderid );	
						} else {
							logger.info("Product order is scheduled to start later, orderid= " + orderid );
						}
					}
					
				}
			}	
		}
		
		execution.setVariable("productOrdersToBeProcessed", ordersToBeProcessed);
		


	}
}
