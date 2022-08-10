package com.function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class UpdateAirlinePreferences {
    /**
     * This function listens at endpoint "/api/updateAirlinePreferences". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/UpdateAirlinePreferences
     * 2. curl {your host}/api/UpdateAirlinePreferences?name=HTTP%20Query
     */
    @FunctionName("updateAirlinePreferences")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("airline");
        String airline = request.getBody().orElse(query);

        query = request.getQueryParameters().get("airlineKey");
        String airlineKey = request.getBody().orElse(query);

        query = request.getQueryParameters().get("value");
        String value = request.getBody().orElse(query);

        query = request.getQueryParameters().get("role");
        String role = request.getBody().orElse(query);

        if (airline == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body").build();
        } else {
            ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .build();

            SecretClient client = new SecretClientBuilder()
                    .vaultUrl("https://fda-groundservices-kv.vault.azure.net/")
                    .credential(managedIdentityCredential)
                    .buildClient();

            KeyVaultSecret dbUri = client.getSecret("SQLDatabaseUrlNew");
            context.getLogger().info(dbUri.getValue());

            int resultSet = 0;

            try (Connection connection = DriverManager.getConnection(dbUri.getValue());
                    Statement statement = connection.createStatement();) {

                // Create and execute a SELECT SQL statement.
                String selectSql = "UPDATE airline_preferences SET " + role + " = '" + value + "' WHERE airline = 'airline-" + airline + "' AND airline_key = '" + airlineKey + "'";
                context.getLogger().info(selectSql);

                resultSet = statement.executeUpdate(selectSql);
                context.getLogger().info(String.valueOf("Rows updated: " + resultSet));
               
            } catch (Exception e) {
                System.out.println(e);
            }

            return request.createResponseBuilder(HttpStatus.OK).body(String.valueOf(resultSet)).build();
        }
    }
}
