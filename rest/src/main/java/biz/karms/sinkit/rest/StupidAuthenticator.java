package biz.karms.sinkit.rest;

import javax.ejb.Singleton;

/**
 * @author Michal Karm Babacek
 */
@Singleton
public class StupidAuthenticator {

    // TODO: Obviously, this is just for playing around.
    // We will construct a proper challenge-response later...
    private String accessToken = System.getenv("SINKIT_ACCESS_TOKEN");

    public boolean isAuthenticated(String token) {
        return (token != null && accessToken != null && accessToken.equals(token));
    }
}
