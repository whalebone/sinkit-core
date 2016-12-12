package biz.karms.sinkit.rest;

/**
 * @author Michal Karm Babacek
 */
public class StupidAuthenticator {
    // TODO: Obviously, this is just for playing around.
    // We will construct a proper challenge-response later...
    private static final String accessToken = System.getenv("SINKIT_ACCESS_TOKEN");
    private static final boolean ignoreAccessToken = Boolean.parseBoolean(System.getenv("SINKIT_IGNORE_ACCESS_TOKEN"));

    private StupidAuthenticator() {
    }

    public static boolean isAuthenticated(String token) {
        if(ignoreAccessToken) {
            return true;
        }
        return (token != null && accessToken != null && accessToken.equals(token));
    }
}
