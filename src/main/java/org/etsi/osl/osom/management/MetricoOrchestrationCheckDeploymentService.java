package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "metricoOrchestrationCheckDeploymentService")
public class MetricoOrchestrationCheckDeploymentService implements JavaDelegate {
    private static final transient Log logger = LogFactory.getLog(MetricoOrchestrationCheckDeploymentService.class.getName());

    @Value("${spring.application.name}")
    private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

    @Override
    public void execute(DelegateExecution execution) {

        logger.info( "MetricoOrchestrationService" );
        logger.info( execution.getVariableNames().toString() );

        if ( execution.getVariable("contextServiceId") == null) {

            logger.error( "Variable contextServiceId is NULL!" );
            execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
            return;
        }
        Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );

        if ( aService == null ) {
            logger.info( "aService is null for contextServiceId = " +(String) execution.getVariable("contextServiceId") );
            execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
            return;
        }

        execution.setVariable("serviceDeploymentFinished", Boolean.FALSE );

        ServiceUpdate supd = new ServiceUpdate();
        boolean propagateToSO = false;


    }
}
