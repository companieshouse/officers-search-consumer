#!/bin/bash
#
# Start script for officers-search-consumer

PORT=8081
exec java -jar -Dserver.port="${PORT}" "officers-search-consumer.jar"
