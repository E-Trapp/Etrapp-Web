package cat.udl.etrapp.server.controllers;

import cat.udl.etrapp.server.db.DBManager;
import cat.udl.etrapp.server.models.*;
import cat.udl.etrapp.server.utils.Password;
import cat.udl.etrapp.server.utils.Utils;
import com.sun.istack.internal.Nullable;

import java.sql.*;

import static cat.udl.etrapp.server.utils.Utils.getHashedString;

public class UsersDAO {

    private static UsersDAO instance;

    private UsersDAO() {

    }

    public static synchronized UsersDAO getInstance() {
        if (instance == null) instance = new UsersDAO();
        return instance;
    }

    public User createUser(UserAuth userAuth) {
        User user = null;

        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users (username, password_hashed, token) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        ) {
            SessionToken tokenData = Utils.generateSessionToken();

            statement.setString(1, userAuth.getUsername());
            statement.setString(2, Password.hashPassword(userAuth.getPassword()));
            statement.setString(3, tokenData.getHashedToken());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getLong(1));
                    user.setUsername(userAuth.getUsername());
                    user.setToken(tokenData.getPlainToken());
                }
            } catch (SQLException e) {
                System.err.println("Error in SQL: createUser()");
                System.err.println(e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Error in SQL: createUser()");
            System.err.println(e.getMessage());
            return null;
        }
        return user;
    }

    @Nullable
    public User getUserById(long id) {
        User user = null;
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, username, token FROM users WHERE id = ?");
        ) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    user = new User();
                    user.setId(resultSet.getLong("id"));
                    user.setToken(resultSet.getString("token"));
                    user.setUsername(resultSet.getString("username"));
                }
            } catch (SQLException e) {
                System.err.println("Error in SQL: getUserById()");
                System.err.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error in SQL: getUserById()");
            System.err.println(e.getMessage());
        }
        return user;
    }

    public UserInfo getUserInfoById(long id) {
        return UserInfo.fromUser(getUserById(id));
    }

    @Nullable
    public User getUserByToken(String tokenPlainText) {
        User user = null;

        final String token = getHashedString(tokenPlainText.getBytes());

        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, username, token FROM users WHERE token = ?");
        ) {
            statement.setString(1, token);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    user = new User();
                    user.setId(resultSet.getLong("id"));
                    user.setToken(resultSet.getString("token"));
                    user.setUsername(resultSet.getString("username"));
                }
            } catch (SQLException e) {
                System.err.println("Error in SQL: getUserById()");
                System.err.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error in SQL: getUserById()");
            System.err.println(e.getMessage());
        }

        return user;
    }

    public void updateToken(String token, long id) {
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET token = ? WHERE id = ?");
        ) {
            statement.setString(1, token);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error in SQL: updateToken()");
            System.err.println(e.getMessage());
        }
    }

    public boolean validateToken(String tokenPlainText) {

        final String token = getHashedString(tokenPlainText.getBytes());

        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE token = ?");
        ) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            } catch (SQLException e) {
                System.err.println("Error in SQL: validateToken()");
                System.err.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error in SQL: validateToken()");
            System.err.println(e.getMessage());
        }
        return false;
    }

    public User authenticate(Credentials credentials) {
        User user = null;
        // "SELECT id, username, password_hashed FROM users WHERE username = ?"
        // Password.checkPassword(credentials.getPassword(), resultSet.get..(password_hashed)..
        // updateToken();

        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, password_hashed FROM users WHERE username = ?");
        ) {
            statement.setString(1, credentials.getUsername());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {

                    final String hashed_password = resultSet.getString("password_hashed");

                    if (Password.checkPassword(credentials.getPassword(), hashed_password)) {
                        user = new User();
                        SessionToken tokenData = Utils.generateSessionToken();
                        user.setToken(tokenData.getPlainToken());
                        updateToken(tokenData.getHashedToken(), resultSet.getLong("id"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error in SQL: getUserById()");
                System.err.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error in SQL: getUserById()");
            System.err.println(e.getMessage());
        }
        return user;

    }

    public boolean deauthenticate(String token) {
        boolean deauth = false;
        User user = getUserByToken(token);
        if (user != null && user.getToken().equals(getHashedString(token.getBytes()))) {
            updateToken(null, user.getId());
            deauth = true;
        }
        return deauth;
    }


}
