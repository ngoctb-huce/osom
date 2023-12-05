import org.etsi.osl.tmf.scm633.model.ServiceSpecification;

public class LcmTest1 {

	public static ServiceSpecification run(ServiceSpecification spec) {

		//SNIP START
		if (getCharValNumber("Video quality of the mobile cameras") == 3) {
			  setCharValNumber("Maximum Number of cameras", 10);
			  setCharValString("Open5GCore-2enb_nsd::OSM_CONFIG", """
			  {
			      "nsdId": "59065615-9b6b-4344-8432-1eb7975a37e7",
			      "vimAccountId": "eb0db325-6cc0-4763-813e-0d06ff754a4e",
			      "vnf": [
			          {
			              "member-vnf-index": "1",
			              "vdu": [
			                  {
			                      "id": "PrometheusCharmedVNF-VM",
			                      "interface": [
			                          {
			                              "name": "eth0",
			                              "floating-ip-required": true
			                          }
			                      ]
			                  }
			              ]
			          }
			      ],
			      "vld": [
			          {
			              "name": "public",
			              "vim-network-name": "OSMFIVE_selfservice01"
			          },
			          {
			              "name": "network1",
			              "vim-network-name": "provider10_vlan401"
			          },
			          {
			              "name": "network3",
			              "ip-profile": {
			                  "ip-version": "ipv4",
			                  "subnet-address": "192.168.101.0/24",
			                  "gateway-address": "0.0.0.0",
			                  "dns-server": [
			                      {
			                          "address": "8.8.8.8"
			                      }
			                  ],
			                  "dhcp-params": {
			                      "enabled": true
			                  }
			              },
			              "vnfd-connection-point-ref": [
			                  {
			                      "member-vnf-index-ref": "1",
			                      "vnfd-connection-point-ref": "vnf-cp3",
			                      "ip-address": "192.168.101.100"
			                  }
			              ]
			          }
			      ],
			      "additionalParamsForVnf": [
			          {
			              "member-vnf-index": "1",
			              "additionalParams": {
			                  "Target1": "val1",
			                  "Target2": "val2",
			                  "Target3": "val3"
			              }
			          }
			      ]
			  }
			  """);
			} else if (getCharValNumber("Video quality of the mobile cameras") == 2) {
			  setCharValNumber("Maximum Number of cameras", 20);
			  setCharValString("Open5GCore-2enb_nsd::OSM_CONFIG", """
			  {
			      "nsdId": "59065615-9b6b-4344-8432-1eb7975a37e7",
			      "vimAccountId": "eb0db325-6cc0-4763-813e-0d06ff754a4e",
			      "vnf": [
			          {
			              "member-vnf-index": "1",
			              "vdu": [
			                  {
			                      "id": "PrometheusCharmedVNF-VM",
			                      "interface": [
			                          {
			                              "name": "eth0",
			                              "floating-ip-required": true
			                          }
			                      ]
			                  }
			              ]
			          }
			      ],
			      "vld": [
			          {
			              "name": "public",
			              "vim-network-name": "OSMFIVE_selfservice01"
			          },
			          {
			              "name": "network1",
			              "vim-network-name": "provider10_vlan401"
			          },
			          {
			              "name": "network2",
			              "vim-network-name": "provider10_vlan1000"
			          },
			          {
			              "name": "network3",
			              "ip-profile": {
			                  "ip-version": "ipv4",
			                  "subnet-address": "192.168.101.0/24",
			                  "gateway-address": "0.0.0.0",
			                  "dns-server": [
			                      {
			                          "address": "8.8.8.8"
			                      }
			                  ],
			                  "dhcp-params": {
			                      "enabled": true
			                  }
			              },
			              "vnfd-connection-point-ref": [
			                  {
			                      "member-vnf-index-ref": "1",
			                      "vnfd-connection-point-ref": "vnf-cp3",
			                      "ip-address": "192.168.101.100"
			                  }
			              ]
			          },
			          {
			              "name": "network4",
			              "ip-profile": {
			                  "ip-version": "ipv4",
			                  "subnet-address": "192.168.102.0/24",
			                  "gateway-address": "0.0.0.0",
			                  "dns-server": [
			                      {
			                          "address": "8.8.8.8"
			                      }
			                  ],
			                  "dhcp-params": {
			                      "enabled": true
			                  }
			              },
			              "vnfd-connection-point-ref": [
			                  {
			                      "member-vnf-index-ref": "1",
			                      "vnfd-connection-point-ref": "vnf-cp4",
			                      "ip-address": "192.168.102.100"
			                  }
			              ]
			          },
			          {
			              "name": "network5",
			              "ip-profile": {
			                  "ip-version": "ipv4",
			                  "subnet-address": "192.168.103.0/24",
			                  "gateway-address": "0.0.0.0",
			                  "dns-server": [
			                      {
			                          "address": "8.8.8.8"
			                      }
			                  ],
			                  "dhcp-params": {
			                      "enabled": true
			                  }
			              },
			              "vnfd-connection-point-ref": [
			                  {
			                      "member-vnf-index-ref": "1",
			                      "vnfd-connection-point-ref": "vnf-cp5",
			                      "ip-address": "192.168.103.100"
			                  }
			              ]
			          }
			      ],
			      "additionalParamsForVnf": [
			          {
			              "member-vnf-index": "1",
			              "additionalParams": {
			                  "Targets": "172.16.10.203,172.16.10.205,192.168.101.101"
			              }
			          }
			      ]
			  }
			  """);
			} else {
			  setCharValNumber("Maximum Number of cameras", 100);
			  setCharValString("Open5GCore-2enb_nsd::OSM_CONFIG", """
			  {
			      "nsdId": "59065615-9b6b-4344-8432-1eb7975a37e7",
			      "vimAccountId": ""
			  }
			  """);
			}
			if (getCharValString("image of Network Assistanse Server (NASS)").equals("test")==true) {
			  setCharValString("image of Network Assistanse Server (NASS)", "HIGHAVAIL");
			}

		
		//SNIP END
		spec.setName( spec.getName() + "_changed Maximum Number of cameras!" );	
		return spec;
	}

	private static String getCharValString(String s) {
		return s;
	}


	private static void setCharValString(String string, String string2) {
		// TODO Auto-generated method stub
		
	}

	private static void setCharValNumber(String string, int i) {
		// TODO Auto-generated method stub
		
	}

	private static int getCharValNumber(String string) {
		// TODO Auto-generated method stub
		return 0;
	}

}
