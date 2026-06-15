package com.hospital.transfer.controller;

import com.hospital.transfer.dto.ApiResponse;
import com.hospital.transfer.dto.BedQueryResponse;
import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.enums.BedType;
import com.hospital.transfer.enums.IsolationType;
import com.hospital.transfer.service.BedService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beds")
public class BedController {

    private final BedService bedService;

    public BedController(BedService bedService) {
        this.bedService = bedService;
    }

    @PostMapping
    public ApiResponse<BedQueryResponse> createBed(@Valid @RequestBody Bed bed) {
        Bed created = bedService.createBed(bed);
        return ApiResponse.success("床位创建成功", toQueryResponse(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<BedQueryResponse> updateBed(@PathVariable Long id, @RequestBody Bed bed) {
        Bed updated = bedService.updateBed(id, bed);
        return ApiResponse.success("床位更新成功", toQueryResponse(updated));
    }

    @GetMapping("/{id}")
    public ApiResponse<BedQueryResponse> getBed(@PathVariable Long id) {
        Bed bed = bedService.getBed(id);
        return ApiResponse.success(toQueryResponse(bed));
    }

    @GetMapping
    public ApiResponse<List<BedQueryResponse>> getAllBeds() {
        return ApiResponse.success(bedService.getAllBeds());
    }

    @GetMapping("/available")
    public ApiResponse<List<BedQueryResponse>> getAvailableBeds(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) BedType bedType,
            @RequestParam(required = false) IsolationType isolationType) {

        if (department != null && bedType != null) {
            return ApiResponse.success(bedService.getAvailableBedsByDepartmentAndType(department, bedType));
        }
        if (department != null && isolationType != null) {
            return ApiResponse.success(bedService.getAvailableBedsByDepartmentAndIsolation(department, isolationType));
        }
        if (department != null) {
            return ApiResponse.success(bedService.getAvailableBedsByDepartment(department));
        }
        return ApiResponse.success(bedService.getAllAvailableBeds());
    }

    @GetMapping("/count/available")
    public ApiResponse<Long> countAvailableBeds(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) BedType bedType,
            @RequestParam(required = false) IsolationType isolationType) {

        if (department != null && bedType != null) {
            return ApiResponse.success(bedService.countAvailableBedsByType(department, bedType));
        }
        if (department != null && isolationType != null) {
            return ApiResponse.success(bedService.countAvailableBedsByIsolation(department, isolationType));
        }
        if (department != null) {
            return ApiResponse.success(bedService.countAvailableBeds(department));
        }
        if (bedType != null) {
            return ApiResponse.success(bedService.countAvailableBedsOnlyByType(bedType));
        }
        if (isolationType != null) {
            return ApiResponse.success(bedService.countAvailableBedsOnlyByIsolation(isolationType));
        }
        return ApiResponse.success(bedService.countAllAvailableBeds());
    }

    @GetMapping("/available/ward/{wardNumber}")
    public ApiResponse<List<BedQueryResponse>> getAvailableBedsByWard(
            @RequestParam String department,
            @PathVariable String wardNumber) {
        return ApiResponse.success(bedService.getAvailableBedsByWard(department, wardNumber));
    }

    @GetMapping("/count/available/ward/{wardNumber}")
    public ApiResponse<Long> countAvailableBedsByWard(
            @RequestParam String department,
            @PathVariable String wardNumber) {
        return ApiResponse.success(bedService.countAvailableBedsByWard(department, wardNumber));
    }

    private BedQueryResponse toQueryResponse(Bed bed) {
        return BedQueryResponse.builder()
                .id(bed.getId())
                .bedNumber(bed.getBedNumber())
                .department(bed.getDepartment())
                .bedType(bed.getBedType())
                .isolationType(bed.getIsolationType())
                .occupied(bed.getOccupied())
                .occupiedByAdmissionNumber(bed.getOccupiedByAdmissionNumber())
                .roomNumber(bed.getRoomNumber())
                .wardNumber(bed.getWardNumber())
                .build();
    }
}
