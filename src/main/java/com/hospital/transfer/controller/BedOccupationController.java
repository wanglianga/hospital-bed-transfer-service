package com.hospital.transfer.controller;

import com.hospital.transfer.dto.ApiResponse;
import com.hospital.transfer.dto.TransferResponse;
import com.hospital.transfer.entity.FamilyNotification;
import com.hospital.transfer.entity.NurseStationConfirmation;
import com.hospital.transfer.service.BedOccupationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/occupation")
public class BedOccupationController {

    private final BedOccupationService bedOccupationService;

    public BedOccupationController(BedOccupationService bedOccupationService) {
        this.bedOccupationService = bedOccupationService;
    }

    @PutMapping("/{transferId}/confirm")
    public ApiResponse<TransferResponse> confirmOccupation(@PathVariable Long transferId) {
        return ApiResponse.success("占床确认成功", bedOccupationService.confirmOccupation(transferId));
    }

    @PutMapping("/{transferId}/admission")
    public ApiResponse<TransferResponse> confirmAdmission(@PathVariable Long transferId) {
        return ApiResponse.success("入科确认成功", bedOccupationService.confirmAdmission(transferId));
    }

    @PostMapping("/{transferId}/nurse-confirm")
    public ApiResponse<NurseStationConfirmation> nurseStationConfirm(
            @PathVariable Long transferId,
            @RequestParam String nurseId,
            @RequestParam String nurseName,
            @RequestParam(defaultValue = "true") Boolean accepted,
            @RequestParam(required = false) String rejectReason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime estimatedArrivalTime,
            @RequestParam(required = false) String remark) {
        return ApiResponse.success("护士站确认成功",
                bedOccupationService.nurseStationConfirm(transferId, nurseId, nurseName, accepted,
                        rejectReason, estimatedArrivalTime, remark));
    }

    @PostMapping("/{transferId}/notify-family")
    public ApiResponse<FamilyNotification> notifyFamily(
            @PathVariable Long transferId,
            @RequestParam String familyContact,
            @RequestParam String content) {
        return ApiResponse.success("家属通知已发送",
                bedOccupationService.notifyFamily(transferId, familyContact, content));
    }

    @PutMapping("/{transferId}/family-response")
    public ApiResponse<FamilyNotification> familyResponse(
            @PathVariable Long transferId,
            @RequestParam Boolean agreed,
            @RequestParam(required = false) String refusalReason) {
        return ApiResponse.success("家属回应已记录",
                bedOccupationService.familyResponse(transferId, agreed, refusalReason));
    }

    @PutMapping("/{transferId}/release")
    public ApiResponse<TransferResponse> releaseOccupation(
            @PathVariable Long transferId,
            @RequestParam String reason) {
        return ApiResponse.success("床位已释放", bedOccupationService.releaseOccupation(transferId, reason));
    }
}
