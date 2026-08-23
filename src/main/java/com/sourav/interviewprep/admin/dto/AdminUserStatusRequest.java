package com.sourav.interviewprep.admin.dto;

import com.sourav.interviewprep.auth.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(@NotNull AccountStatus status) {
}
