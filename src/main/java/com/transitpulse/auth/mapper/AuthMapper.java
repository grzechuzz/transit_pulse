package com.transitpulse.auth.mapper;

import com.transitpulse.auth.dto.AuthUserResponse;
import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    AuthUserResponse toResponse(User user);

    AuthUserResponse toResponse(AuthenticatedUser user);
}
