package com.example;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * by preecha.d 09/11/2025
 */
public class JwtUtil {

	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtUtil.class);
	private static Algorithm algorithm = null;

	static {
		String secretKey = "mysecret123";
		algorithm = Algorithm.HMAC256(secretKey);
	}

	public static String getJwt() {
		//==== สร้าง jwt
		// ใช้ secret key สำหรับ HS256
		String token = JWT.create()
				.withIssuer("my-api")
				.withSubject("user123")
				.withClaim("role", "admin")
				.withNotBefore(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + 3600_000)) // 1 ชม.
				.sign(algorithm);

		log.info("token: {} ", token);
		return token;
	}

	/**
	 * Verify a JWT
	 * @param token
	 */
	public static boolean verifyToken(String token) {
		try {
			log.info("token: {} ", token);
			JWTVerifier verifier = JWT.require(algorithm)
					// specify any specific claim validations
					.withIssuer("my-api") //ถ้าใส่ไม่ตรงจะเกิด error
					// reusable verifier instance
					.build();
			DecodedJWT decodedJWT = verifier.verify(token);
			log.info("getNotBefore: {} ", decodedJWT.getNotBefore());
			log.info("getExpiresAt: {} ", decodedJWT.getExpiresAt());

			if (decodedJWT.getExpiresAt().after(new Date(System.currentTimeMillis()))) {
				return true;
			} else {
				log.info("The token has expired.");
				return false;
			}

		} catch (JWTVerificationException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	public static void main(String[] args) {
		String token = getJwt();
		verifyToken(token);
	}
}
