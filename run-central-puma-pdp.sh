#!/bin/bash

java -jar target/puma-central-puma-pdp.jar --policy-home "$(pwd)/resources/policies/" -s true
