package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.model.nfv.ScaleDescriptor;
import org.etsi.osl.osom.management.AlarmsService;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.EValueType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "NFVODAY2config") //bean name
public class NFVODAY2config implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( NFVODAY2config.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	AlarmsService alarmsService;
	
	public void execute(DelegateExecution execution) {
		
		logger.debug("NFVODAY2config:" + execution.getVariableNames().toString() );
		
			ObjectMapper mapper = new ObjectMapper();
			ServiceActionQueueItem item;
			Service aService;
			Service originalService;
			try {
				item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
				aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
				//extract the original service from the Item

				originalService =  mapper.readValue( item.getOriginalServiceInJSON() , Service.class);;
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
				return;
			}
			
			
			
			List<Characteristic> changeCharacteristics = new ArrayList<>();
			//send to mano client here: only the modified action!
			//identify here the characteristics that changed
			if ( aService.getServiceCharacteristic()!=null ) {
				for (Characteristic srcChar : aService.getServiceCharacteristic()) {
					
						if ( originalService.getServiceCharacteristicByName( srcChar.getName() )!= null ) {
							
							Characteristic origChar = originalService.getServiceCharacteristicByName( srcChar.getName() );
							if ( ( origChar !=null ) && ( origChar.getValue() !=null ) && ( origChar.getValue().getValue() !=null )) {
								if ( !origChar.getValue().getValue().equals(srcChar.getValue().getValue()) ) {
									changeCharacteristics.add( srcChar );									
								}
							}							
						}
				}						
			}
			
			
			
			
			Note n = new Note();
			n.setText("Service Action NFVODAY2config. Action: " + item.getAction() +". " );
			n.setAuthor( compname);
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			
			
			String nsInstanceId = "";
			if ( aService.getServiceCharacteristicByName( "InstanceId" )!=null) {
				nsInstanceId= aService.getServiceCharacteristicByName( "InstanceId" ).getValue().getValue() ;
			} else {
				logger.error("NFVODAY2config: InstanceId is NULL."  );
				
			}
			

			ServiceUpdate supd = new ServiceUpdate();
			
			String ncTxt = "";
			for (Characteristic characteristic : changeCharacteristics) {
				ncTxt += characteristic.getName() + ", ";

				
				if ( ncTxt.toUpperCase().contains(  "PRIMITIVE::" ) ) {
					if ( (characteristic != null ) && (characteristic.getValueType() != null ) && characteristic.getValueType().equals("ARRAY") ) {

						NSActionRequestPayload nsp = new NSActionRequestPayload();
						nsp.setNsInstanceId(nsInstanceId);
						
						
						String characteristicValue = characteristic.getValue().getValue();
						List<Any> vals = new ArrayList<>();
						try {
							vals = mapper.readValue( characteristicValue, new TypeReference<List<Any>>() {});
						} catch (JsonProcessingException e) {
							e.printStackTrace();
							n.setText( n.getText() + characteristicValue + "\nERROR\n" + e.getOriginalMessage() );
							
						}

						logger.debug("NFVODAY2config: characteristicValue = " +characteristicValue );
						for ( Any actionValue : vals) {
							if ( actionValue.getAlias().equals("primitive") ) {
								nsp.setPrimitive( actionValue.getValue() );
							} else if ( actionValue.getAlias().equals("member_vnf_index") ) {
								nsp.setVnf_member_index( actionValue.getValue() );
							} else if ( actionValue.getAlias().equals("vdu_id") ) {
								nsp.setVdu_id( actionValue.getValue() );
							} else if ( actionValue.getAlias().equals("vdu_count_index") ) {
								nsp.setVdu_count_index( actionValue.getValue() );
							} else {

								nsp.getPrimitive_params().put( actionValue.getAlias() , actionValue.getValue());
							}
						}
						
						if ( nsp.getPrimitive() != null ) {
							/**
							 * {
								"nsInstanceId": "8a3db62a-eb0e-48d9-be9b-548f7f034512",
								"member_vnf_index": "?",
								"primitive": "?",
								"primitive_params": {
									"?": "?"
								}
							}
							 */
							
							String payload="NO_PAYLOAD";
							try {
								payload = mapper.writeValueAsString(nsp);
							} catch (JsonProcessingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	
							logger.debug("NFVODAY2config NSActionRequestPayload= " + payload );						
							String actionresult = serviceOrderManager.nfvoDay2Action(nsp);
							if ( actionresult.contains("ACCEPTED") ) {
								n.setText( n.getText() + "ACCEPTED" );
								
							} else {
								n.setText( n.getText() + " " + actionresult );								
							}
						}
						
						
						
					}
				} else if ( ncTxt.toUpperCase().contains(  "EXEC_ACTION" ) ) {
					String characteristicValue = characteristic.getValue().getValue();
					Map<String, String> vals = new HashMap<>();
					try {
						vals = mapper.readValue( characteristicValue, new TypeReference< Map<String, String>>() {});
					} catch (JsonProcessingException e1) {
						e1.printStackTrace();
						n.setText( n.getText() + characteristicValue + "\nERROR\n" + e1.getOriginalMessage() );
					}
					logger.debug("NFVODAY2config:  EXEC_ACTION characteristicValue = " +characteristicValue );
					//first add to a new item the acknowledge
					Characteristic characteristicAck = new Characteristic();
					characteristicAck.setName("EXEC_ACTION_LAST_ACK");
					characteristicAck.setValueType(  EValueType.TEXT.getValue()  );
					
					if (  vals.get("ACTION_NAME") != null) {
						if ( vals.get("ACTION_NAME").equalsIgnoreCase("scaleServiceEqually") ) {

							ScaleDescriptor aScaleDescriptor = new ScaleDescriptor();
							aScaleDescriptor.setNsInstanceId(nsInstanceId);
							aScaleDescriptor.setMemberVnfIndex(  vals.get("Member_vnf_index") );
							aScaleDescriptor.setScalingGroupDescriptor(vals.get("Scaling_group_descriptor"));
							aScaleDescriptor.setScaleVnfType( "SCALE_OUT" );
							
							String actionresult = serviceOrderManager.nfvoScaleDescriptorAction( aScaleDescriptor );
							logger.debug("NFVODAY2config: actionresult = " +actionresult );

							if ( actionresult != null ) {
								if ( actionresult.contains("202") ) {
									n.setText( n.getText() + "ACCEPTED. Values=" + vals.toString());
									characteristicAck.setValueType(  characteristicAck.getValueType()  );
									Any value = new Any();
									value.setValue( characteristicValue );		
									characteristicAck.setValue( value );
								} else {
									n.setText( n.getText() + " " + actionresult );		
									characteristicAck.setValueType(  "TEXT"  );
									Any value = new Any();
									value.setValue( "ERROR" );		
									characteristicAck.setValue( value );
								}
								
							} else {

								n.setText( n.getText() + " ERROR ON NFVODAY2config" );		
								characteristicAck.setValueType(  "TEXT"  );
								Any value = new Any();
								value.setValue( "ERROR" );		
								characteristicAck.setValue( value );			
							}
							
						} else if ( vals.get("ACTION_NAME").equalsIgnoreCase("execDay2") ) {
							NSActionRequestPayload nsp = new NSActionRequestPayload();
							nsp.setNsInstanceId(nsInstanceId);
							try {
								for (String valkey : vals.keySet() ) {
									if ( valkey.equals("primitive") ) {
										nsp.setPrimitive( vals.get("primitive") ); // e.g. fsetup									
									} else if ( valkey.equals("member_vnf_index") ) {
										nsp.setVnf_member_index( vals.get("member_vnf_index") ); // e.g. fsetup									
									} else if ( valkey.equals("vdu_id") ) {
										nsp.setVdu_id( vals.get("vdu_id") ); // e.g. fsetup									
									}else if ( valkey.equals("vdu_count_index") ) {
										nsp.setVdu_count_index( vals.get("vdu_count_index") ); // e.g. fsetup									
									}else  if ( valkey.equals("params") ) {
										String[] params = vals.get("params").split(";");
										for (String prm : params) {
											String[] p = prm.split("=");
											nsp.getPrimitive_params().put( p[0] , p[1]  );
										}
									}								
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							if ( nsp.getPrimitive() != null ) {
								/**
								 * {
									"nsInstanceId": "8a3db62a-eb0e-48d9-be9b-548f7f034512",
									"member_vnf_index": "?",
									"primitive": "?",
									"primitive_params": {
										"?": "?"
									}
								}
								 */
								
								String payload="NO_PAYLOAD";
								try {
									payload = mapper.writeValueAsString(nsp);
								} catch (JsonProcessingException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}	
								logger.debug("ACTION_NAME execDay2 NFVODAY2config NSActionRequestPayload= " + payload );						
								String actionresult = serviceOrderManager.nfvoDay2Action(nsp);
								if ( actionresult.contains("ACCEPTED") ) {
									n.setText( n.getText() + "ACCEPTED for ACTION_NAME execDay2" );
									characteristicAck.setValueType(  "TEXT"  );
									Any value = new Any();
									value.setValue( "ACCEPTED" );		
									characteristicAck.setValue( value );
									
								} else {
									n.setText( n.getText() + " " + actionresult );		
									characteristicAck.setValueType(  "TEXT"  );
									Any value = new Any();
									value.setValue( "ERROR" );		
									characteristicAck.setValue( value );						
								}
							}
							
							
						}
						
					}
					
					
					
					supd.addServiceCharacteristicItem(characteristicAck);					
					
				}
				
				
			}
			
			
			supd.addNoteItem( n );
			serviceOrderManager.deleteServiceActionQueueItem( item );			
			serviceOrderManager.updateService( aService.getId() , supd, false);

			logger.debug("NFVODAY2config:" + n.getText() );
			
		
		
	}

}
