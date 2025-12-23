#!/bin/bash

# alternative way of running the tests.
# script starts the conjur quickstart via docker compose and do all required configuration
# after running it, please export  all 6 variables and then you can start the test

echo "Temporary folder 'tmp' is used"
cd tmp
cd conjur-quickstart
echo "Removing previous instances"
docker-compose rm -fsv
