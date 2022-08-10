package com.function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.function.pojos.UserPreferences;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class GetUserPreferences {
    /**
     * This function listens at endpoint "/api/getUserPreferences". Two ways to
     * invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/getUserPreferences
     * 2. curl {your host}/api/getUserPreferences?name=HTTP%20Query
     */
    @FunctionName("getUserPreferences")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("airline");
        String airline = request.getBody().orElse(query);

        if (airline == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body").build();
        } else {
            String conn = System.getenv("DB_CONNECTION_STRING");

            ResultSet resultSet = null;
            List<UserPreferences> preferences = new ArrayList<UserPreferences>();

            try (Connection connection = DriverManager.getConnection(conn);
                    Statement statement = connection.createStatement();) {

                // Create and execute a SELECT SQL statement.
                String selectSql = "SELECT * FROM user_preferences WHERE airline = 'airline-"+ airline +"'";
                context.getLogger().info(selectSql);

                resultSet = statement.executeQuery(selectSql);
                context.getLogger().info(resultSet.toString());

                // Print results from select statement
                while (resultSet.next()) {
                    UserPreferences preference = new UserPreferences();
                    preference.setPreference(resultSet.getString("PREFERENCE"));
                    preference.setDescription(resultSet.getString("DESCRIPTION"));
                    preference.setEnabled(resultSet.getBoolean("ENABLED"));
                    preference.setToggle(resultSet.getBoolean("TOGGLE"));
                    preference.setValue(resultSet.getString("VALUE"));
                    preference.setUserKey(resultSet.getString("USER_KEY"));
                    preferences.add(preference);

                }
            } catch (Exception e) {
                System.out.println(e);
            }


            return request.createResponseBuilder(HttpStatus.OK).body(preferences).build();
        }
    }
}
