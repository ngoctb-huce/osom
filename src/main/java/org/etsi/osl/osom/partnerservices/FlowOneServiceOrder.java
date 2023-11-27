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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;

@JsonIgnoreProperties(ignoreUnknown=true)
public class FlowOneServiceOrder  {

	@JsonProperty("id")
	private String id = null;


	@JsonProperty("state")
	private String state;


	@JsonProperty("orderItem")
	private ServiceOrderItem orderItem ;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}


	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}


	/**
	 * @return the orderItem
	 */
	public ServiceOrderItem getOrderItem() {
		return orderItem;
	}


	/**
	 * @param orderItem the orderItem to set
	 */
	public void setOrderItem(ServiceOrderItem orderItem) {
		this.orderItem = orderItem;
	}


	
	
}
