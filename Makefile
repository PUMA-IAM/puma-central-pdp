all:
	mvn clean package
	scp target/puma-central-puma-pdp.jar ubuntu@dnetcloud-tomcat:/home/ubuntu
