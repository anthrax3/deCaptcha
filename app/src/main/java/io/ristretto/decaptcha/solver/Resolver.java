package io.ristretto.decaptcha.solver;

import java.net.URL;

/**
 * Created by coffeemakr on 12.01.17.
 */

public interface Resolver {
    void resolve(URL url);
}
