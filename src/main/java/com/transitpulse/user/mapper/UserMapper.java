package com.transitpulse.user.mapper;

import com.transitpulse.user.dto.UserResponse;
import com.transitpulse.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
