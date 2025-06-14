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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;



/**
 * @author ctranoris
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class SimpleIDSpec extends ServiceSpecification{


	@JsonProperty("id")
	protected long id;

	/**
	 * @return the id
	 */
//	@Override
//	public long getId() {
//		return id;
//	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	@JsonIgnore
	public String getIntAsString() {
		return id + "";
	}
	
	
	
}
