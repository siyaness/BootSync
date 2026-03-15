package com.bootsync.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiAttendanceSaveRequest(
    @NotBlank String status,
    @Size(max = 200) String memo
) {
}
