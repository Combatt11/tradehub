package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.User;

/**
 * Puerto de salida para emitir tokens JWT.
 */
public interface JwtTokenPort {

    String generateToken(User user);

    long expirationSeconds();
}
