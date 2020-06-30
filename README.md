# JaCaMo-hypermedia

This project implements a [JaCaMo](https://github.com/jacamo-lang/jacamo) bridge for [Yggdrasil](https://github.com/interactions-hsg/yggdrasil).

## Prerequisites

- JDK 8+

## Getting started

Clone this project with:

```
git clone --recursive git@github.com:Interactions-HSG/jacamo-hypermedia.git
```

Run `./gradlew`

## Mocking HTTP requests

One simple solution for mocking HTTP requests is [MockServer](https://www.mock-server.com/):

1. Add the expected HTTP responses in `mockserver/mockserver.json`. The format of an expectation is given in the [MockServer OpenAPI specification](https://app.swaggerhub.com/apis/jamesdbloom/mock-server-openapi/5.10.x#/Expectation).

2. Run MockServer with [Docker](https://www.docker.com/). To use the expectation initialization file created in the previous step, you will have to use a bind mount and to set an environment variable like so:

```
docker run -v "$(pwd)"/mockserver/mockserver.json:/tmp/mockserver/mockserver.json \
-e MOCKSERVER_INITIALIZATION_JSON_PATH=/tmp/mockserver/mockserver.json \
-d --rm --name mockserver -p 1080:1080 mockserver/mockserver
```

The above command will run the Docker container in the background and will print the container ID. To stop the container: `docker stop CONTAINER_ID` 
