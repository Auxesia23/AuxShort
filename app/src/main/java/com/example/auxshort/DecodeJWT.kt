import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT

data class JwtPayload(
    val email: String?,
    val iat: Long?,
    val name: String?,
    val picture: String?
)

fun decodeJwtPayload(token: String): JwtPayload {
    val decodedJWT: DecodedJWT = JWT.decode(token)

    return JwtPayload(
        email = decodedJWT.getClaim("email").asString(),
        iat = decodedJWT.getClaim("iat").asLong(),
        name = decodedJWT.getClaim("name").asString(),
        picture = decodedJWT.getClaim("picture").asString()
    )
}
