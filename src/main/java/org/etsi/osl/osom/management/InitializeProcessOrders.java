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
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "initializeProcessOrders") // bean name
public class InitializeProcessOrders implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(InitializeProcessOrders.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;

	@Autowired
	private RuntimeService runtimeService;

	public void execute(DelegateExecution execution) {

		logger.info("Check if there are new Service Orders for Orchestration: " + execution.getVariables().toString());

		if (execution.getVariable("orderid") instanceof String) {
			logger.info("Task has available orderid = " + execution.getVariable("orderid"));
			execution.setVariable("orderid", execution.getVariable("orderid"));// get the first one

		}

		if (execution.getVariable("ordersToBeProcessed") instanceof ArrayList) {

			List<String> ordersToBeProcessed = (ArrayList<String>) execution.getVariable("ordersToBeProcessed");
			for (String oId : ordersToBeProcessed) {

					
				logger.debug("Will send CAMEL Message that Order is IN-PROGRESS orderid= " + oId );					

				ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();
				serviceOrderUpd.setState(ServiceOrderStateType.INPROGRESS);
				serviceOrderManager.updateServiceOrderOrder( oId, serviceOrderUpd );						
					
				
				
			}

		}

	}
}
