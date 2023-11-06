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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;

@Component(value = "fetchInProgressOrders") // bean name
public class FetchInProgressOrders implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchInProgressOrders.class.getName());


    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	private TaskService taskService;

	
	public void execute(DelegateExecution execution) {
		logger.info("======================Fetch InProgressOrders from Service Order Repository, procid:" + execution.getProcessDefinitionId()  + "======================================");		

		List<String> orderlist = serviceOrderManager.retrieveOrdersByState( ServiceOrderStateType.INPROGRESS );
		ArrayList<String> ordersToBeProcessed = new ArrayList<>();
		if ( orderlist != null ) {
			for (String orderid : orderlist) {				
					ordersToBeProcessed.add( orderid );
			}	
		}
		
		execution.setVariable("ordersToBeQueried", ordersToBeProcessed);
		
		
	}
}
