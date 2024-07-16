/*-
 * ========================LICENSE_START=================================
 * org.etsi.osl.sol007nbi.osm
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


package org.etsi.osl.osom.serviceactions;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author ichatzis
 *
 */
public class NSActionRequestPayload 
{	
	
    private String nsInstanceId;
	private String member_vnf_index; // Needed for VNFD mapping NSD.constituent-vnfd.member-vnf-index. If missing a NSD is targeting. Example: "member_vnf_index": "1",
	private String vdu_id; // Needed if target is a VDU. Maps VNFD.vdu-id
	private String vdu_count_index; // Needed if the VDU has several replicas (count>1)
	private String primitive; // Mandatory. Maps [NSD.ns-configuration or VNFD.vnf-configuration or VNFD.vdu.vdu-configuration].config-primitive.name. Example: "primitive": "touch"
	private String param;	
    private String lcmOperationType; //"lcmOperationType": "action"
    private String vnf_member_index; // TODO for backward compatibility. To remove in future
    private String kdu_name;
    private Integer timeout_ns_action;
    private Map<String, Object> primitive_params = new LinkedHashMap<>();
    
    public Map<String, Object> getPrimitive_params() {
		return primitive_params;
	}

	@JsonAnySetter
	public void setPrimitive_params(Map<String, Object> primitive_params) {
		this.primitive_params = primitive_params;
	}
    
	public String getMember_vnf_index() {
		return member_vnf_index;
	}

	public void setMember_vnf_index(String member_vnf_index) {
		this.member_vnf_index = member_vnf_index;
	}

	public String getVdu_id() {
		return vdu_id;
	}

	public void setVdu_id(String vdu_id) {
		this.vdu_id = vdu_id;
	}

	public String getVdu_count_index() {
		return vdu_count_index;
	}

	public void setVdu_count_index(String vdu_count_index) {
		this.vdu_count_index = vdu_count_index;
	}

	public String getPrimitive() {
		return primitive;
	}

	public void setPrimitive(String primitive) {
		this.primitive = primitive;
	}

	public void setPrimitiveParams(String primitive_name, String primitive_value) {
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public String getLcmOperationType() {
		return lcmOperationType;
	}

	public void setLcmOperationType(String lcmOperationType) {
		this.lcmOperationType = lcmOperationType;
	}

	public String getNsInstanceId() {
		return nsInstanceId;
	}

	public void setNsInstanceId(String nsInstanceId) {
		this.nsInstanceId = nsInstanceId;
	}

	public String getVnf_member_index() {
		return vnf_member_index;
	}

	public void setVnf_member_index(String vnf_member_index) {
		this.vnf_member_index = vnf_member_index;
	}

	public String getKdu_name() {
		return kdu_name;
	}

	public void setKdu_name(String kdu_name) {
		this.kdu_name = kdu_name;
	}

	public Integer getTimeout_ns_action() {
		return timeout_ns_action;
	}

	public void setTimeout_ns_action(Integer timeout_ns_action) {
		this.timeout_ns_action = timeout_ns_action;
	}

	public String toJSON()
	{
		String jsonInString=null;
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		try {
			jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return jsonInString;
	}    
}

