echo "Building PIP utils"
cd ../puma-pip-utils
mvn -q clean install
echo "Building PDP RMI utils"
cd ../puma-central-puma-pdp-rmi-utils
mvn -q clean install
echo "Building SunXACML...:"
cd ../puma-sunxacml
mvn -q clean install
echo "Building central PDP..."
cd ../puma-central-puma-pdp
mvn -q clean install
cd target/
scp puma-central-puma-pdp.jar ubuntu@dnetcloud-central:~
cd ..
