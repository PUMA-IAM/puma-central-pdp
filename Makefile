dnetcloud:
	mvn clean package
	scp target/puma-central-puma-pdp.jar ubuntu@dnetcloud-tomcat:/home/ubuntu

simac:
	mvn clean package
	scp target/puma-central-puma-pdp.jar ubuntu@sis3s-puma:/home/ubuntu
  
dais:
	mvn clean package
	scp target/puma-central-puma-pdp.jar fedora@dais-puma-demo:/home/fedora
