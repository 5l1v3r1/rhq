rem This script should consume all the wsdls and compile the JAXB types all
rem into one directory.

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/RoleManagerBean?wsdl -p org.rhq.enterprise.server.ws

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/ContentManagerBean?wsdl -p org.rhq.enterprise.server.ws

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/SubjectManagerBean?wsdl -p org.rhq.enterprise.server.ws

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/OperationManagerBean?wsdl -p org.rhq.enterprise.server.ws

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/RepoManagerBean?wsdl -p org.rhq.enterprise.server.ws

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/ConfigurationManagerBean?wsdl -p org.rhq.enterprise.server.ws

call ../../../../dev-container/jbossas/bin/wsconsume.bat -k http://127.0.0.1:7080/rhq-rhq-server/ResourceManagerBean?wsdl -p org.rhq.enterprise.server.ws