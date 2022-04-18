package com.function;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Health {
    /**
     * This function listens at endpoint "/api/health". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/health
     * 2. curl {your host}/api/health?name=HTTP%20Query
     */
    @FunctionName("health")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        context.getLogger().info("status: UP");
        return request.createResponseBuilder(HttpStatus.OK).body("status: UP").build();

    }
}
