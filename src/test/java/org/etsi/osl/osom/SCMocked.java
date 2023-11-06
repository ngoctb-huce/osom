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
package org.etsi.osl.osom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.etsi.osl.model.DeploymentDescriptor;
import org.etsi.osl.model.DeploymentDescriptorStatus;
import org.etsi.osl.model.NetworkServiceDescriptor;
import org.etsi.osl.tmf.lcm.model.LCMRuleSpecification;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import jakarta.validation.Valid;

public class SCMocked {


	private static final transient Log logger = LogFactory.getLog(SCMocked.class.getName());


	private static Map<String, org.etsi.osl.tmf.sim638.model.Service> runningServices = new HashMap<>();
	
	
	private DeploymentDescriptor requeestedDescriptor;
	
	/**
	 * get mocked service order by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public String getOrderById(String id) throws IOException {
		logger.info( "getOrderById id= " + id );
		File sspec = new File( "src/test/resources/TestExServiceOrder.json" );
		InputStream in = new FileInputStream( sspec );
		String sspectext = IOUtils.toString(in, "UTF-8");
		
		return sspectext;
	}

	
	
	/**
	 * 
	 * get mocked service spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public String getSpecById(String id)  throws IOException {

		logger.info( "getSpecById id= " + id );
		String sspectext = null;
		if ( id.equals( "f2b74f90-4140-4895-80d1-ef243398117b" )) {
			File sspec = new File( "src/test/resources/TestExBundleSpec.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "59d08753-e1b1-418b-9e3e-d3a3bb573051" )) {
			File sspec = new File( "src/test/resources/TestExSpec1.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "a7ff2349-ccab-42a1-9212-51d4a1912247" )) {
			File sspec = new File( "src/test/resources/TestExSpec2.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "0d5551e6-069f-43b7-aa71-10530f290239" )) {
			File sspec = new File( "src/test/resources/TestExSpec3.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "93b9928c-de35-4495-a157-1100f6e71c92" )) {
			File sspec = new File( "src/test/resources/TestServiceOrderDates.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "c00446ac-c8af-47ad-ac94-518d4bdd4c13" )) {
			File sspec = new File( "src/test/resources/TestServiceNSD.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "22e399d3-b152-4966-9d0f-20e5b2ec42c4" )) {
			File sspec = new File( "src/test/resources/NFVO_Example_RFS.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "0399516f-e9ae-4c8e-8f7a-b13ad9a1bd00" )) {
			File sspec = new File( "src/test/resources/NFVO_Example.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "99176116-17cf-464f-96f7-86e685914666" )) {
			File sspec = new File( "src/test/resources/cirros_2vnf_ns_RFS.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		}
		
		
		
		return sspectext;
	}
	
	public String getMockedService()  throws IOException {
		logger.info( "getMockedService()"  );

		String sspectext = null;
		
		File sspec = new File( "src/test/resources/TestService.json" );
		InputStream in = new FileInputStream( sspec );
		sspectext = IOUtils.toString(in, "UTF-8");
		return sspectext;		
	}
	
	
	public String getMockedAddService(@Valid String aservice)  throws IOException {
		//normally this is a ServiceCreate
		
		org.etsi.osl.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)aservice, org.etsi.osl.tmf.sim638.model.Service.class); 
		
		logger.info( "getMockedAddService() service name: " + serviceInstance.getName()  );

		serviceInstance.setUuid( UUID.randomUUID().toString() );
		
		runningServices.put(serviceInstance.getUuid() , serviceInstance);
		logger.info( "getMockedAddService() runningServices.sizes: " + runningServices.size()  );
		
		String sspectext = null;
		
		sspectext = toJsonString(serviceInstance);
		return sspectext;		
	}
	
	
	public String updateServiceOrder(String so)  throws IOException {

		logger.info( "updateServiceOrder so= " + so );
		
		ServiceOrder sorder = toJsonObj(so, ServiceOrder.class);
		
		
		String sspectext = null;
//		
//		File sspec = new File( "src/test/resources/TestService.json" );
//		InputStream in = new FileInputStream( sspec );
//		sspectext = IOUtils.toString(in, "UTF-8");

		sspectext = toJsonString(sorder);
		return sspectext;		
	}

	
	public String getServiceById(String id)  throws IOException {

		logger.info( "getServiceById id = " + id );

		String sspectext = null;
		
		if ( runningServices.get(id) != null ) {
			org.etsi.osl.tmf.sim638.model.Service serviceInstance = runningServices.get(id);
			sspectext = toJsonString(serviceInstance);
			return sspectext;		
		}
		
		
		File sspec = new File( "src/test/resources/TestService.json" );
		InputStream in = new FileInputStream( sspec );
		sspectext = IOUtils.toString(in, "UTF-8");
		return sspectext;		
	}
	
	public String getLCMRulebyID(String id) throws IOException {		
		logger.info( "getLCMRulebyID id = " + id );

		String sspectext = null;
		
		if ( id.equals("40f027b5-24a9-4db7-b422-a963c9feeb7a") ) {
			File sspec = new File( "src/test/resources/LcmCirrosRule1Test.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");
			return sspectext;			
		}else if ( id.equals("75cebf16-1699-486f-8304-d6512f90c910") ) {
			File sspec = new File( "src/test/resources/LcmCirrosRule2Test.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");
			return sspectext;			
		} else if ( id.equals("8b7b8339-0c33-4731-af9c-c98adadbe777") ) {
			File sspec = new File( "src/test/resources/LcmCirrosRule3Test.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");
			return sspectext;			
			
		} else if ( id.equals("49e2e679-9dc1-4c7b-abd9-72377d4c1a5d") ) {
			File sspec = new File( "src/test/resources/LcmRule4Test.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");
			return sspectext;			
		} else if ( id.equals("c1bd362d-011f-485b-a7d9-3bb05a2f6868") ) {
			File sspec = new File( "src/test/resources/LcmRule5Test.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");
			return sspectext;			
		}
		
		
		
		
		return "";			
	}
	
	public LCMRuleSpecification getLCMRulebyIDJson(String id) throws IOException {		
		String s = getLCMRulebyID( id); 
		
		return toJsonObj(s, LCMRuleSpecification.class);
	}

	public String getLCMRulesbySpecIDPhase(String specid, String phaseName) throws IOException {	
		logger.info( "getLCMRulesbySpecIDPhase specid = " + specid );	
				
		String sspectext = null;

		if ( specid.equals("f2b74f90-4140-4895-80d1-ef243398117b") ) {
			File sspec = new File( "src/test/resources/LcmRuleListSpecTest.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");
			return sspectext;		
			
		}
		

		return "[]";	
	}
	
	public String req_deploy_nsd( String ddreq )  throws IOException {

		logger.info( "ddreq getExperiment = " + toJsonString(ddreq) );
		
		DeploymentDescriptor ddresp = toJsonObj( ddreq, DeploymentDescriptor.class);
		ddresp.setId(123456789);
		setRequeestedDescriptor(ddresp);
		return toJsonString(ddresp);		
	}
	
	
	public String req_deployment_id( Long ddreqId )  throws IOException {

		logger.info( "ddreq get id = " + ddreqId );
		
		DeploymentDescriptor ddresp = new DeploymentDescriptor();
		ddresp.setId(ddreqId);
		ddresp.setStatus( DeploymentDescriptorStatus.RUNNING );
		return toJsonString(ddresp);		
	}
	
	
	public String req_nsd_id( Long ddreqId )  throws IOException {

		logger.info( "req_nsd_id = " + ddreqId );
		
		NetworkServiceDescriptor sor = new NetworkServiceDescriptor();
		sor.setId(ddreqId);
		return toJsonString(sor);		
	}
	
	public String getServiceQueueItems() throws IOException {		
		return "[]";		
	}
	
	 static String toJsonString(Object object) throws IOException {
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	        return mapper.writeValueAsString(object);
	    }

	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }



	public DeploymentDescriptor getRequeestedDescriptor() {
		return requeestedDescriptor;
	}



	public void setRequeestedDescriptor(DeploymentDescriptor requeestedDescriptor) {
		this.requeestedDescriptor = requeestedDescriptor;
	}



	/**
	 * @return the runningServices
	 */
	public Map<String, org.etsi.osl.tmf.sim638.model.Service> getRunningServices() {
		return runningServices;
	}


	
}
