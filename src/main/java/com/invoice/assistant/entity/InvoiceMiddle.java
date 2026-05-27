package com.invoice.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_sim_original_bill")
public class InvoiceMiddle {

    @TableId(type = IdType.INPUT)
    private String id;

    private String billNo;
    private LocalDate billDate;
    private Integer invoiceProperty;
    private String invoiceType;
    private String remark;
    private String buyerName;
    private String buyerTaxpayerId;
    private String buyerBankAndAccount;
    private String buyerAddressAndTel;
    private String buyerRecipientMail;
    private Integer buyerProperty;
    private String orgCode;
    private String sellerName;
    private String sellerTaxpayerId;
    private String sellerBankAndAccount;
    private String sellerAddressAndTel;
    private BigDecimal totalAmount;
    private Integer includeTaxFlag;
    private String blueinvoiceCode;
    private String blueinvoiceNo;
    private Integer redReason;
    private LocalDateTime originalIssueTime;
    private String blueInvoiceType;
    private Integer autoInvoice;
    private String username;

    @TableField("tu25_producttype")
    private Integer tu25Producttype;

    @TableField("tu25_productbatchno")
    private String tu25Productbatchno;

    @TableField("tu25_producedate")
    private LocalDate tu25Producedate;

    @TableField("tu25_validperiod")
    private String tu25Validperiod;

    @TableField("tu25_productorigin")
    private String tu25Productorigin;

    @TableField("tu25_provinceserialno")
    private String tu25Provinceserialno;

    private String drawer;

    /** 是否已开票：0-未开票，1-已开票，2-开票失败 */
    @TableField("is_invoicing")
    private Integer isInvoicing;

    // ===== 开票后返回字段 =====
    private String interfaceCode;
    private String returnCode;
    private String returnMsg;
    private String invoiceStatus;
    private String invoiceCode;
    private String invoiceNum;
    private LocalDate invoiceDate;
    private String invoiceFileUrl;
    private String invoiceImageUrl;
    private String invoicePdfFileUrl;
    private String invoiceXmlFileUrl;
    private String orderNo;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
