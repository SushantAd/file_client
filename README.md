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
2. Akka HTTP based application exposing a single route with method get /api/server/get-or-create/{{requestId}}.
3. Use Concurrent Hashmap to manage throttle keep track of request and resource, in case 
4. Store file in a central location.

####Assumptions:
1. Throttling will be based on server request time.
2. RequestId is used to identify unique resource.

####Acceptance Criteria:
1. When a user calls the API, a file is created in a central location and empty response for a unique requestId with 202 status code is returned.
2. When a user calls the API with previous requestId, file is found in central location and fileContent is returned with 200 status code. 
3. When a user calls the API and error occurs in the server, an empty response with 500 (Internal error) status code is returned.


## Appendix

### Running

You need to download and install sbt for this application to run.

Once you have sbt installed, type/run the following command in the terminal:

Pre-requisite
1. Please change central-directory to a proper directory where the file is to be saved, we are not force creating directories at this point.
   file_store{
   central-directory = "C:\\centralDirClient"
   default-extension = "txt"
   }

```bash
sbt run
or
Run via IDE
```
Url: http://127.0.0.1:9090/api/client/get-or-create/testRequest1

###Limitation:
1. Throttle is based on server processing time (Reason- Server takes a min 5s, which would lead to multiple retries call to the server).
2. Max retry is currently not set and will keep on retrying after 500ms. (Can be easily extended to add maxRetries)

###Extensions:
1. Throttle can be extended by adding timestamp check when fetching from cache.
2. Unit and Integrations tests can be better used for edge cases.


###Important - For Testing
To run Unit test:
```bash
sbt test
or
Run test via IDE
```

To run Integration test:
Prerequisites:
1. File-Server should be online or will throw 500 error. 
2. File-Client-Server should be online or will throw 500 error.
3. Using single conf file so testing will also create file in the same central directory (will not work if central dir value is wrong in config.)
4. File will be created and deleted after IT is completed. (File name: "t_e_s_t_file_dont_use.txt")
```bash
sbt it:test
or
Run it test via IDE
```

