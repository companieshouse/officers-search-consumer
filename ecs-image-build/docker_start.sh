#!/bin/bash
#
# Start script for officers-search-consumer

PORT=8080
exec java -jar -Dserver.port="${PORT}" "officers-search-consumer.jar"
