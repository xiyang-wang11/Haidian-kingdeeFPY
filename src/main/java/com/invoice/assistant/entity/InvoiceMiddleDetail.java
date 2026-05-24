package com.invoice.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_sim_original_bill_item")
public class InvoiceMiddleDetail {

    @TableId(type = IdType.INPUT)
    private String id;

    private String billid;
    private BigDecimal amount;
    private Integer originalSeq;
    private String goodsCode;
    private String goodsName;
    private String lineProperty;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    private String privilegeContent;
    private String privilegeFlag;
    private String revenueCode;
    private String specification;
    private BigDecimal discountAmount;
    private String units;
    private String remark;
    private String extraField;
    private String extraField2;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
