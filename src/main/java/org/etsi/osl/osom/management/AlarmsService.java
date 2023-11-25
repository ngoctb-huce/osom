package org.etsi.osl.osom.management;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.am642.model.AlarmCreate;
import org.etsi.osl.tmf.am642.model.AlarmStateType;
import org.etsi.osl.tmf.am642.model.AlarmUpdate;
import org.etsi.osl.tmf.am642.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlarmsService {

	private static final transient Log logger = LogFactory.getLog(AlarmsService.class.getName());

	@Autowired
	CamelContext contxt;

	@Autowired
	ProducerTemplate template;

	@Value("${ALARMS_ADD_ALARM}")
	private String ALARMS_ADD_ALARM ="";

	@Value("${ALARMS_UPDATE_ALARM}")
	private String ALARMS_UPDATE_ALARM ="";

	@Value("${spring.application.name}")
	private String compname;
	
	/**
	 * update the alarm status in alarm repo
	 * @param cleared 
	 * 
	 * @param alarm
	 * @param canBehandled
	 * @param actions
	 */
	public void patchAlarmClear(String alarmId, String textNote, boolean cleared ) {
		AlarmUpdate aupd = new AlarmUpdate();
		Comment comment = new Comment();
		comment.setTime(OffsetDateTime.now(ZoneOffset.UTC));
		comment.setSystemId(compname);
		
		if ( cleared ) {
			aupd.setClearSystemId( compname );
			comment.setComment("Alarm cleared by applying actions." + textNote);
			aupd.setState(AlarmStateType.cleared.name());				
		} else {
			comment.setComment("Alarm updated, not cleared." + textNote);
		}
			
				

		aupd.addCommentItem(comment);		

		try {
			logger.info("Alarm id = " + alarmId + "." + comment.getComment());
			String response = this.updateAlarm(aupd, alarmId);
			logger.info("Alarm id updated = " + response.toString() );
		} catch (IOException e) {
			logger.error("patchAlarmClear Alarm id = " + alarmId );
			e.printStackTrace();
		}
	}
	
	/**
	 * @param al
	 * @return a response in string
	 * @throws IOException
	 */
	public String createAlarm(AlarmCreate al) throws IOException {
			
		String body;
		body = toJsonString(al);
		logger.info("createAlarm body = " + body);
		Object response = template.requestBody( ALARMS_ADD_ALARM, body);
		return response.toString();
	}
	
	/**
	 * @param al
	 * @return a response in string
	 * @throws IOException
	 */
	public String updateAlarm(AlarmUpdate al, String alarmid) throws IOException {
			
		String body;
		body = toJsonString(al);
		logger.info("updateAlarm body = " + body);
		Object response = template.requestBodyAndHeader( ALARMS_UPDATE_ALARM, body , "alarmid", alarmid);
		return response.toString();
	}
	
	

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}

}
