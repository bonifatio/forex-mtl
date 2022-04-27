# Local Forex Proxy

## Running One-Frame service locally:

`docker-compose up -d`

## Running Forex proxy application:

`sbt run`

By default service works on port 8080 and accepts requests of the form

`GET /rates?from=JPY&to=USD`

## Running tests

`sbt test`

## Implementation details

Implementation uses in-memory cache using Caffeine library. Cache allows us to make 
requests to API only if respective entry is missed. As a key `from` currency is used and value is a list
of all possible rates for that currency. Cache adds entry with a TTL so it's responsibility of the cache
to remove it afterwards.

For 9 supported currencies maximum number of requests to the API is expected to be 288*9, which is very 
low compared to a limitation.
