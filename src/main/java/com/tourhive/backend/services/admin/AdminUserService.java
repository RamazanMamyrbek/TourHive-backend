package com.tourhive.backend.services.admin;

import com.tourhive.backend.dto.admin.request.BlockUserRequestDto;
import com.tourhive.backend.dto.admin.request.UpdateUserRoleRequestDto;
import com.tourhive.backend.dto.admin.response.AdminUserDetailResponseDto;
import com.tourhive.backend.dto.admin.response.AdminUserResponseDto;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {

    Page<AdminUserResponseDto> getUsers(String search, Role role, UserStatus status, Pageable pageable);

    AdminUserDetailResponseDto getUserDetails(UUID userId);

    AdminUserResponseDto blockUser(UUID adminId, String adminEmail, UUID userId, BlockUserRequestDto request, String ipAddress);

    AdminUserResponseDto unblockUser(UUID adminId, String adminEmail, UUID userId, String ipAddress);

    AdminUserResponseDto updateUserRole(UUID adminId, String adminEmail, UUID userId, UpdateUserRoleRequestDto request, String ipAddress);
}
