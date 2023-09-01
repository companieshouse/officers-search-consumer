# Officers Search Consumer

## Summary

* The company appointments consumer consumes messages from the stream-company-officers Kafka topic and deserialises them
  using the ResourceChangeData avro schema. 
* The messages are then processed by sending a get request to Company Appointments API 
  to get a list of appointments to upsert into the primary search index. 
* If these appointments are found and it is an upsert request then the search.api.ch.gov endpoint is called to upsert 
  them to the primary index. 
* If the appointments are not found and it is a delete request then the delete endpoint of search.api.ch.gov will be 
  called and the appointments will be removed from the primary index. 
* In all other cases the request should error and be sent to the retry, error or invalid topics respectively.

## System requirements

* [Git](https://git-scm.com/downloads)
* [Java](http://www.oracle.com/technetwork/java/javase/downloads)
* [Maven](https://maven.apache.org/download.cgi)
* [MongoDB](https://www.mongodb.com/)
* [Apache Kafka](https://kafka.apache.org/)

## Building and Running Locally using Docker

1. Clone [Docker CHS Development](https://github.com/companieshouse/docker-chs-development) and follow the steps in the
   README.
2. Enable the following services using the command `./bin/chs-dev services enable <service>`.
    * officers-search-consumer
    * search-api-ch-gov-uk
3. Enable the streaming module using the command `./bin/chs-dev modules enable streaming`
4. Boot up the services' containers on docker using tilt `tilt up`.
5. Messages can be produced to the stream-company-officers using the instructions given in
   [CHS Kafka API](https://github.com/companieshouse/chs-kafka-api).

## Environment variables

| Variable                                      | Description                                                                         | Example (from docker-chs-development)                    |
|-----------------------------------------------|-------------------------------------------------------------------------------------|----------------------------------------------------------|
| CHS_API_KEY                                   | The client ID of an API key with internal app privileges                            | abc123def456ghi789                                       |
| API_URL                                       | The URL which the Company Appointments API is hosted on                             | http://api.chs.local:4001                                |
| PAYMENTS_API_URL                              | The URL which the payments service is hosted on                                     | http://api-payments.chs.local:4001                       |
| SERVER_PORT                                   | The server port of this service                                                     | 9090                                                     |
| BOOTSTRAP_SERVER_URL                          | The URL to the kafka broker                                                         | kafka:9092                                               |
| CONCURRENT_LISTENER_INSTANCES                 | The number of listeners run in parallel for the consumer                            | 1                                                        |
| STREAM_COMPANY_OFFICERS_TOPIC                 | The topic ID for stream company officers kafka topic                                | stream-company-officers                                  |
| GROUP_ID                                      | The group ID for the services Kafka topics                                          | officers-search-consumer                                 |
| MAX_ATTEMPTS                                  | The number of times a message will be retried before being moved to the error topic | 5                                                        |
| BACKOFF_DELAY                                 | The incremental time delay between message retries                                  | 100                                                      |
| STREAM_COMPANY_PROFILE_INVALID_MESSAGE_TOPIC  | The full name of the stream company profile invalid topic                           | stream-company-officers-officers-search-consumer-invalid |
| LOGLEVEL                                      | The level of log messages output to the logs                                        | debug                                                    |
| HUMAN_LOG                                     | A boolean value to enable more readable log messages                                | 1                                                        |

## Building the docker image

    mvn compile jib:dockerBuild -Dimage=169942020521.dkr.ecr.eu-west-1.amazonaws.com/local/officers-search-consumer

## To make local changes

Development mode is available for this service
in [Docker CHS Development](https://github.com/companieshouse/docker-chs-development).

    ./bin/chs-dev development enable officers-search-consumer

This will clone the officers-search-consumer into the repositories folder. Any changes to the code, or resources will
automatically trigger a rebuild and relaunch.