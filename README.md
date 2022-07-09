# file_client
A minimal file server-client based on Akka Http making Http client calls to its counter part file_server.
The project has been built keeping in mind, a service that can be built within approx 4-5 hours.

###  Requirements: Create HTTP service (client) with a single endpoint:

GET /api/client/get-or-create/{{requestId}}
which
1. retrieves the value of a resource from the upstream service (server) as soon as it becomes available or with a delay of max 500ms
2. stores the value in a file
3. no longer calls the server if a resources is available in a local file and responds with the value
from that file
4. client service should throttle requests for a specific resource (as per server rate limit - never
more than 2 requests / second / resource)
5. The endpoint responds immediately with either

Output:
A:
HTTP response code 202 (Accepted) if the resource value is not yet available
or B:
{"requestId”: "{{S1}}", "fileContent": "{{S2}}"} - if the resource is available in a local file

####Suggested Solution:
1. Idiomatic approach, using custom code than Akka streams.
2. Akka HTTP based application exposing a single route with Post method get /api/server/get-or-create/{{requestId}}.
3. Use Akka Actor for async calls and adding 5s delay.
4. Store file in a central location.

####Assumptions:


####Acceptance Criteria:
1. When a user calls the API, a file is created in a central location and empty response for a unique requestId with 202 status code is returned.
2. When a user calls the API with previous requestId, file is found in central location and fileContent is returned with 200 status code. 
3. When a user calls the API and error occurs in the server, an empty response with 500 (Internal error) status code is returned.


## Appendix

### Running

You need to download and install sbt for this application to run.

Once you have sbt installed, type/run the following command in the terminal:

```bash
sbt run
or
Run via IDE
```

Limitation:

Extensions:

####Note:
