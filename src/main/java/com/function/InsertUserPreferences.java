package com.function;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.pojos.UserPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class InsertUserPreferences {
    /**
     * This function listens at endpoint "/api/InsertAirlinePreferences". Two ways
     * to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/InsertAirlinePreferences
     * 2. curl {your host}/api/InsertAirlinePreferences?name=HTTP%20Query
     */
    @FunctionName("insertUserPreferences")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String airline = request.getQueryParameters().get("airline");
        String updatedBy = request.getQueryParameters().get("updatedBy");
        String data = request.getBody().orElse(null);
        context.getLogger().info(data);


        Gson gson = new Gson();
        Type preferencesListType = new TypeToken<ArrayList<UserPreferences>>() {
        }.getType();
        List<UserPreferences> preferences = gson.fromJson(data, preferencesListType);

        String conn = System.getenv("DB_CONNECTION_STRING");

        try (Connection connection = DriverManager.getConnection(conn);
                Statement statement = connection.createStatement();) {

            PreparedStatement pstmt = connection
                    .prepareStatement("INSERT INTO user_preferences values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (UserPreferences preference : preferences) {
                if (preference.getGroupBy().contains("Units")) {
                    pstmt.setString(1, airline);
                    pstmt.setString(2, preference.getPreference());
                    pstmt.setString(3, preference.getUserKey());
                    pstmt.setBoolean(4, preference.getEnabled());
                    pstmt.setString(5, preference.getDescription());
                    pstmt.setString(6, preference.getGroupBy());
                    pstmt.setBoolean(7, preference.getToggle());
                    pstmt.setString(8, preference.getValue());
                    pstmt.setString(9, updatedBy);
                    pstmt.setTimestamp(10, new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()));
                    pstmt.executeUpdate();
                }
            }

            context.getLogger().info("User preferences for [" + airline + "] inserted by " + updatedBy);

        } catch (Exception e) {
            context.getLogger().info("Error: " + e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inserting user preferences").build();

        }
        return request.createResponseBuilder(HttpStatus.OK).body("User preferences inserted successfully").build();
    }

}
