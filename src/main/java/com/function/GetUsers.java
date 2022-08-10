package com.function;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.function.pojos.UserAccount;
import com.microsoft.azure.functions.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Azure Functions with HTTP Trigger.
 */
public class GetUsers {
    /**
     * This function listens at endpoint "/api/getUsers". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/GetUsers
     * 2. curl {your host}/api/GetUsers?name=HTTP%20Query
     * 
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    @FunctionName("getUsers")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws SQLException, ClassNotFoundException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("airline");
        String airline = request.getBody().orElse(query);

        query = request.getQueryParameters().get("objectId");
        String objectId = request.getBody().orElse(query);

        if (airline == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass an airline on the query string or in the request body").build();
        } else {

            context.getLogger().info(airline);

            String conn = System.getenv("DB_CONNECTION_STRING");

            ResultSet resultSet = null;
            List<UserAccount> users = new ArrayList<UserAccount>();

            try (Connection connection = DriverManager.getConnection(conn);
                    Statement statement = connection.createStatement();) {

                // Create and execute a SELECT SQL statement.
                String selectSql = "SELECT * FROM [dbo].[user_account_registrations] AS x LEFT JOIN [dbo].[user_tsp_version] AS y ON x.user_object_id = y.user_object_id WHERE airline='airline-"
                        + airline + "' AND x.user_object_id != '" + objectId + "'";
                context.getLogger().info(selectSql);

                resultSet = statement.executeQuery(selectSql);

                // Print results from select statement
                while (resultSet.next()) {
                    UserAccount user = new UserAccount();
                    user.setObjectType("User");
                    user.setObjectId(resultSet.getString("USER_OBJECT_ID"));
                    user.setUserPrincipalName(resultSet.getString("USER_PRINCIPAL_NAME"));
                    String account_state = resultSet.getString("ACCOUNT_STATE");
                    user.setAccountEnabled(account_state.equals("USER_ACTIVATED") ? "true" : "false");
                    user.setDisplayName(resultSet.getString("DISPLAY_NAME"));
                    user.setGivenName(resultSet.getString("FIRST_NAME"));
                    user.setSurname(resultSet.getString("LAST_NAME"));
                    int endPoint = user.getUserPrincipalName().indexOf("@");
                    if (endPoint > 0) {
                        user.setMailNickname(user.getUserPrincipalName().substring(0, endPoint));
                    }
                    String other_email = resultSet.getString("EMAIL_ADDRESS");
                    List<String> emails = new ArrayList<>();
                    emails.add(other_email);
                    user.setOtherMails(emails);
                    user.setUserRole(resultSet.getString("USER_ROLE"));
                    user.setCreatedDateTime(resultSet.getString("REGISTRATION_DATE"));
                    user.setAirline(resultSet.getString("AIRLINE"));
                    user.setVersion(resultSet.getString("VERSION"));
                    user.setLastUpdated(resultSet.getString("LAST_UPDATED"));
                    users.add(user);

                }
            } catch (Exception e) {
                System.out.println(e);
            }

            return request.createResponseBuilder(HttpStatus.OK).body(users).build();
        }
    }
}
