package com.function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.pojos.AirlinePreferences;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class GetAirlinePreferences {
    /**
     * This function listens at endpoint "/api/getAirlinePreferences". Two ways to
     * invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/GetAirlinePreferences
     * 2. curl {your host}/api/GetAirlinePreferences?name=HTTP%20Query
     */
    @FunctionName("getAirlinePreferences")
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
            List<AirlinePreferences> preferences = new ArrayList<AirlinePreferences>();

            try (Connection connection = DriverManager.getConnection(conn);
                    Statement statement = connection.createStatement();) {

                // Create and execute a SELECT SQL statement.
                String selectSql = "SELECT * FROM airline_preferences WHERE airline = 'airline-"+ airline +"'";
                context.getLogger().info(selectSql);

                resultSet = statement.executeQuery(selectSql);
                context.getLogger().info(resultSet.toString());

                // Print results from select statement
                while (resultSet.next()) {
                    AirlinePreferences preference = new AirlinePreferences();
                    preference.setPreference(resultSet.getString("PREFERENCE"));
                    preference.setDescription(resultSet.getString("DESCRIPTION"));
                    preference.setEnabled(resultSet.getBoolean("ENABLED"));
                    preference.setAirlineKey(resultSet.getString("AIRLINE_KEY"));
                    preference.setDisplay(resultSet.getBoolean("DISPLAY"));
                    preference.setChoiceFocal(resultSet.getBoolean("CHOICE_FOCAL"));
                    preference.setChoicePilot(resultSet.getBoolean("CHOICE_PILOT"));
                    preference.setChoiceEFBAdmin(resultSet.getBoolean("CHOICE_EFBADMIN"));
                    preference.setChoiceEFBAdmin(resultSet.getBoolean("CHOICE_EFBADMIN"));
                    preference.setChoiceCheckAirman(resultSet.getBoolean("CHOICE_CHECK_AIRMAN"));
                    preference.setChoiceMaintenance(resultSet.getBoolean("CHOICE_MAINTENANCE"));
                    preferences.add(preference);

                }
            } catch (Exception e) {
                System.out.println(e);
            }

            return request.createResponseBuilder(HttpStatus.OK).body(preferences).build();
        }
    }
}
