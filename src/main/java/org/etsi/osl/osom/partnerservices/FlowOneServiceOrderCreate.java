/*-
 * ========================LICENSE_START=================================
 * org.etsi.osl.osom
 * %%
 * Copyright (C) 2019 - 2020 openslice.io
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
package org.etsi.osl.osom.partnerservices;

import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.so641.model.ServiceOrderCreate;

public class FlowOneServiceOrderCreate extends ServiceOrderCreate{


	public FlowOneServiceOrderCreate(ServiceOrderCreate servOrderCreate) {
		this.setDescription( servOrderCreate.getDescription());
		this.setCategory( servOrderCreate.getCategory());
		this.setOrderItem( servOrderCreate.getOrderItem() );
		this.getOrderItem().get(0).setUuid("1");
		this.getOrderItem().get(0).setBaseType(null);
		this.setRequestedCompletionDate(  servOrderCreate.getRequestedCompletionDate());
		this.setRequestedStartDate( servOrderCreate.getRequestedStartDate());
		this.setRelatedParty(servOrderCreate.getRelatedParty());
		this.setNote( servOrderCreate.getNote());
		this.getNote().get(0).setBaseType(null);
		this.getOrderItem().get(0).state(null);
		this.getOrderItem().get(0).getService().setBaseType(null);
		this.getOrderItem().get(0).getService().getServiceSpecification().setBaseType(null);
		for (Characteristic c : this.getOrderItem().get(0).getService().getServiceCharacteristic()) {
			c.setBaseType(null);
			c.setValueType("string");
		}
		
	}

	
}
