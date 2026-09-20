package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest;
import com.fixlink.adapter.in.web.dto.response.TechnicianProfileResponse;

public interface TechnicianProfileUseCase {
    TechnicianProfileResponse getMyProfile(Long userId);
    TechnicianProfileResponse updateMyProfile(Long userId, UpdateTechnicianProfileRequest request);
}
