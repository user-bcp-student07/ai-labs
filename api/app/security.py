import os

import jwt
from fastapi import Header, HTTPException

JWT_ALGORITHM = "HS256"
JWT_SECRET_ENV = "AUTH_JWT_SECRET"
JWT_ISSUER_ENV = "AUTH_JWT_ISSUER"
JWT_AUDIENCE_ENV = "AUTH_JWT_AUDIENCE"
JWT_MINIMUM_SECRET_BYTES = 32
INVALID_TOKEN_DETAIL = "Invalid token"
AUTH_CONFIGURATION_DETAIL = "Authentication is not configured"


def verified_identity(authorization: str = Header(default="")) -> dict[str, str]:
    scheme, separator, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or not separator or not token.strip():
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL)

    secret = os.getenv(JWT_SECRET_ENV, "")
    issuer = os.getenv(JWT_ISSUER_ENV, "")
    audience = os.getenv(JWT_AUDIENCE_ENV, "")
    if (
        len(secret.encode("utf-8")) < JWT_MINIMUM_SECRET_BYTES
        or not issuer
        or not audience
    ):
        raise HTTPException(status_code=503, detail=AUTH_CONFIGURATION_DETAIL)

    try:
        claims = jwt.decode(
            token.strip(),
            secret,
            algorithms=[JWT_ALGORITHM],
            issuer=issuer,
            audience=audience,
            options={"require": ["exp", "iss", "aud", "sub", "role"]},
        )
    except jwt.PyJWTError as error:
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL) from error

    username = claims.get("sub")
    role = claims.get("role")
    if (
        not isinstance(username, str)
        or not username
        or not isinstance(role, str)
        or not role
    ):
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL)
    return {"username": username, "role": role}
