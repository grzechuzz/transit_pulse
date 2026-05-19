package com.transitpulse.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.transitpulse.common.dto.PageResponse;
import com.transitpulse.user.dto.UserResponse;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.exception.UserNotFoundException;
import com.transitpulse.user.mapper.UserMapper;
import com.transitpulse.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserAdministrationService userAdministrationService;

    @Test
    void getAllReturnsPagedUsers() {
        User user = user(1L, Role.USER);
        UserResponse mapped = response(user);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userMapper.toResponse(user)).thenReturn(mapped);

        PageResponse<UserResponse> response = userAdministrationService.getAll(pageable);

        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1L, response.totalElements());
        assertEquals(mapped, response.content().getFirst());
    }

    @Test
    void updateRoleChangesUserRole() {
        User user = user(1L, Role.USER);
        UserResponse mapped = new UserResponse(1L, user.getEmail(), user.getDisplayName(), Role.MODERATOR, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(mapped);

        UserResponse response = userAdministrationService.updateRole(1L, Role.MODERATOR);

        assertEquals(Role.MODERATOR, user.getRole());
        assertEquals(Role.MODERATOR, response.role());
    }

    @Test
    void updateRoleThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userAdministrationService.updateRole(1L, Role.MODERATOR)
        );
    }

    private static User user(Long id, Role role) {
        User user = new User("user" + id + "@example.com", "password-hash", "User " + id, role);
        user.setId(id);
        return user;
    }

    private static UserResponse response(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
