package com.transitpulse.user.service;

import com.transitpulse.common.dto.PageResponse;
import com.transitpulse.user.dto.UserResponse;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.exception.UserNotFoundException;
import com.transitpulse.user.mapper.UserMapper;
import com.transitpulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdministrationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional
    public UserResponse updateRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        user.setRole(role);

        return userMapper.toResponse(user);
    }
}
