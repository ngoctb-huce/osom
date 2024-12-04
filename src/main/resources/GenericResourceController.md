@startuml

group Controller Startup [Controller Registration]

	control "ResourceController" as ResourceController
	entity "TMFAPI" as TMFAPI
	queue MQa
	
    ResourceController -> MQa: RegisterResourceSpec
    MQa -> TMFAPI: RegisterResourceSpec
    note left
    	Register a ResourceSpec with
    	Name, Category, Version
    end note
    ResourceController -> MQa: QueueRegister
    note left
    	CREATE/<category_name>/<version>
    	UPDATE/<category_name>/<version>
    	DELETE/<category_name>/<version>    
    end note

	
end	


group Create RFS [Create RFS and underlying resource]
	control "ResourceController" as ResourceController
	entity "TMFAPI" as TMFAPI
	queue MQa
	entity "OSOM" as OSOM


    TMFAPI -> OSOM: ServiceOrderCreate

    OSOM -> OSOM: ServiceCreateTMF
    	OSOM->MQa    : ServiceCreateMSG
    	MQa -> TMFAPI: ServiceCreateMSG    	
    	activate TMFAPI
    	return Service 
    	MQa -> OSOM: Service
    	
    	OSOM -> OSOM: ResourceCreateTMF
    	
    	OSOM->MQa    : ResourceCreateMSG
    	MQa -> TMFAPI: ResourceCreate MSG   	
    	activate TMFAPI
    	return Resource 
    	MQa -> OSOM: Resource       
    	
    		
    	OSOM -> OSOM: ResourceDeployment
    	OSOM -> MQa    : CreateGenericResourceMSG
    	note left
    		Header contains
    		more metadata
    		(ServiceID, ResourceID, OrderID)    	    
    	end note
    	MQa -> ResourceController: CreateGenericResourceMSG [CREATE/<category_name>/<version>]
    	
    	OSOM -> OSOM: WaitFor resourceStatus

end


group Resource Controller Process[Process underlying resource]
    	ResourceController -> ResourceController: ProcessRequest

    	ResourceController->MQa    : ResourceUpdate
    	MQa -> TMFAPI: ResourceUpdate   
    	activate TMFAPI
    	return Resource  	
    	MQa -> ResourceController: Resource  

end

group OSOM Check Deployment [Wait for underlying resource]
    	OSOM -> OSOM: WaitFor resourceStatus
    	OSOM->MQa: Check GETResource
    	MQa -> TMFAPI: GETResource  
    	activate TMFAPI
    	return Resource  	
    	MQa -> OSOM: Resource  
    	note left
    		Check resource Status
    		(e.g. ACTIVE or RESERVED or ALARM)   	    
    	end note
end
	

@enduml