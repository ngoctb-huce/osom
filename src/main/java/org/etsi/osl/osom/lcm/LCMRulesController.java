package org.etsi.osl.osom.lcm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.lcm.model.ELCMRulePhase;
import org.etsi.osl.tmf.lcm.model.LCMRuleSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author ctranoris
 *
 */
@Service(value = "lcmRulesController") 
public class LCMRulesController {

	private static final transient Log logger = LogFactory.getLog( LCMRulesController.class.getName() );

    @Autowired
    private ProducerTemplate template;

	
	@Value("${CATALOG_GET_LCMRULE_BY_ID}")
	private String CATALOG_GET_LCMRULE_BY_ID = "";


	@Value("${CATALOG_GET_LCMRULES_BY_SPECID_PHASE}")
	private String CATALOG_GET_LCMRULES_BY_SPECID_PHASE = "";
	

	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	/**
	 * execute rules of a phase
	 * @param phase
	 * @param spec
	 * @param sor
	 * @param serviceToCreate
	 */
	public LCMRulesExecutorVariables execPhase(ELCMRulePhase phase, LCMRulesExecutorVariables vars) {

		logger.debug("In execPhase phase=" + phase 
				+ ", spec name = " + vars.getSpec().getName()
				+ ", orderid = " + vars.getSorder().getId()  );
		
		List<LCMRuleSpecification> lcmspecs = retrieveLCMRulesOfSpecification_Phase( vars.getSpec().getId(), phase.getValue() );
		
		if ( lcmspecs == null) {
			logger.debug("No LCMRuleSpecs for  phase="+ phase 
					+ ", spec name = " + vars.getSpec().getName()
					+ ", orderid = " + vars.getSorder().getId()  );
			return vars;
		}
		
		for (LCMRuleSpecification lcmRuleSpecification : lcmspecs) {
			logger.info("Prepare to execute LCMRuleSpecification =" + lcmRuleSpecification.getName()   );
			LCMRuleSpecification lcmspec = retrieveLCMRuleSpecificationById( lcmRuleSpecification.getId() );

		    LCMRulesExecutor lcmRulesExecutor = new LCMRulesExecutor();
		    vars = lcmRulesExecutor.executeLCMRuleCode(lcmspec, vars);
		}
		
		return vars;
		
	}
	
	
	/**
	 * get  LCMRuleSpecification spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public LCMRuleSpecification retrieveLCMRuleSpecificationById(String lcmspecid) {
		logger.info("will retrieve LCMRuleSpecification from catalog lcmspecid=" + lcmspecid   );
		
		try {
			Object response = template.
					requestBody( CATALOG_GET_LCMRULE_BY_ID, lcmspecid);

			if ( !(response instanceof String)) {
				logger.error("LCMRuleSpecification object is wrong.");
				return null;
			}
			LCMRuleSpecification ls = toJsonObj( (String)response, new TypeReference<LCMRuleSpecification>() {}); 

			return ls;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Specification details from catalog. " + e.toString());
		}
		return null;
	}
	
	
	public List<LCMRuleSpecification> retrieveLCMRulesOfSpecification_Phase(String servicespecid, String phasename ) {
		logger.info("will  retrieveLCMRulesOfSpecification_Phase from catalog serviceSpecid=" + servicespecid   );
		
		try {

			Map<String, Object> map = new HashMap<>();
			map.put("servicespecid", servicespecid );
			map.put("phasename", phasename );
			
			Object response = template.
					requestBodyAndHeaders( CATALOG_GET_LCMRULES_BY_SPECID_PHASE, "",  map);

			if ( !(response instanceof String)) {
				logger.error("LCMRuleSpecifications objects are wrong.");
				return null;
			}
			
			List<LCMRuleSpecification> ls = toJsonObj( (String)response,new TypeReference<List<LCMRuleSpecification>>() {} );
			
			return ls;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Specification details from catalog. " + e.toString());
		}
		return null;
	}
	
	
	static <T> T toJsonObj(String content, TypeReference<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
	
}
