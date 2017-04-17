Move to project folder and run below command (Maven)
BUILD PROJECT : mvn clean compile assembly:single

- Copy jar file in target folder and export.properties file to the same folder.
- Edit export.properties
- Move to this folder.
- run command : java -jar export-domain-jar-with-dependencies.jar <location of ExpiredDomainConfig.txt> <Result Folder>