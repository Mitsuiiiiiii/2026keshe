package com.campuslink.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核申请请求。
 */
@Data
public class AuditDTO {

    /** true 通过 / false 拒绝 */
    @NotNull(message = "请指定审核结果")
    private Boolean approved;

    /** 拒绝理由（拒绝时填写） */
    private String reason;
}
