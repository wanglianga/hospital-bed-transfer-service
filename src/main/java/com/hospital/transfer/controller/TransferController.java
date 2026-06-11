package com.hospital.transfer.controller;

import com.hospital.transfer.dto.ApiResponse;
import com.hospital.transfer.dto.TransferRequest;
import com.hospital.transfer.dto.TransferResponse;
import com.hospital.transfer.enums.PatientStatus;
import com.hospital.transfer.enums.TransferStatus;
import com.hospital.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/apply")
    public ApiResponse<TransferResponse> applyTransfer(@Valid @RequestBody TransferRequest request) {
        return ApiResponse.success("转科申请提交成功", transferService.applyTransfer(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransferResponse> getTransfer(@PathVariable Long id) {
        return ApiResponse.success(transferService.getTransferApplication(id));
    }

    @GetMapping("/patient/{admissionNumber}")
    public ApiResponse<List<TransferResponse>> getByPatient(@PathVariable String admissionNumber) {
        return ApiResponse.success(transferService.getTransfersByPatient(admissionNumber));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<TransferResponse>> getByStatus(@PathVariable TransferStatus status) {
        return ApiResponse.success(transferService.getTransfersByStatus(status));
    }

    @GetMapping("/pending")
    public ApiResponse<List<TransferResponse>> getPendingTransfers() {
        return ApiResponse.success(transferService.getPendingTransfers());
    }

    @GetMapping("/department/{department}")
    public ApiResponse<List<TransferResponse>> getByDepartment(@PathVariable String department) {
        return ApiResponse.success(transferService.getTransfersByDepartment(department));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<TransferResponse> cancelTransfer(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ApiResponse.success("转科申请已取消", transferService.cancelTransfer(id, reason));
    }

    @PutMapping("/{id}/patient-status")
    public ApiResponse<TransferResponse> updatePatientStatus(
            @PathVariable Long id,
            @RequestParam PatientStatus status) {
        return ApiResponse.success("患者状态已更新", transferService.updatePatientStatus(id, status));
    }

    @PostMapping("/retry-pending")
    public ApiResponse<String> retryPendingTransfers() {
        transferService.retryPendingTransfers();
        return ApiResponse.success("已重新尝试分配床位", "done");
    }
}
