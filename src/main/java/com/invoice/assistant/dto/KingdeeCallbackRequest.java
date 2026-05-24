package com.invoice.assistant.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/** 金蝶发票云按单回调请求体（5.1.03） */
@Data
public class KingdeeCallbackRequest {

    /** 业务编码：INVOICE.OPEN-开票，INVOICE.CANCEL-作废，INVOICE.RED-红冲 */
    private String interfaceCode;

    /** 返回编码：0-成功，9999-失败（按单回调不能仅靠此字段判断，需看data内每条记录） */
    private String returnCode;

    /** 返回信息，成功返回success，失败返回原因 */
    private String returnMsg;

    /** 回调数据列表（一个申请单可能拆分为多张发票） */
    private List<CallbackData> data;

    /** 业务管控信息 */
    private BizControl bizControl;

    @Data
    public static class CallbackData {
        private String batch;
        private String billNo;
        private Integer invoiceProperty;
        private String invoiceType;
        private Integer buyerProperty;
        private String buyerName;
        private String buyerTaxpayerId;
        private String buyerAddressAndTel;
        private String buyerBankAndAccount;
        private String buyerRecipientMail;
        private String buyerRecipientPhone;
        private BigDecimal deduction;
        private Integer includeTaxFlag;
        private BigDecimal totalTaxAmount;
        private BigDecimal totalAmount;
        private BigDecimal includeTaxAmount;
        private Integer taxedType;
        private String sellerName;
        private String sellerTaxpayerId;
        private String sellerAddressAndTel;
        private String sellerBankAndAccount;
        private String inventoryMark;
        private String drawer;
        private String reviewer;
        private String payee;
        private String canceler;
        private String abolishReason;
        private String deviceNo;
        private String remark;
        /** 发票状态：0-正常，2-待开，3-红冲，6-作废 */
        private String invoiceStatus;
        /** 发票代码（开票失败时为空，通过此字段判断是否开票成功） */
        private String invoiceCode;
        /** 发票号码 */
        private String invoiceNum;
        /** 开票日期 */
        private String invoiceDate;
        private List<InvoiceDetailItem> invoiceDetail;
        private String invoiceFileUrl;
        private String invoiceImageUrl;
        private String invoicePdfFileUrl;
        private String invoiceXmlFileUrl;
        private String orderNo;
        /** 开票失败原因 */
        private String issueErrorMessage;
        private String originalInvoiceCode;
        private String originalInvoiceNumber;
        private String originalInvoiceStatus;
        private String originalIssueTime;
        private String printFlag;
        private String redInfoBillNo;
        private String serialNo;
        private String systemSource;
        private String terminalNo;
        private String checkCode;
        private String skm;
    }

    @Data
    public static class InvoiceDetailItem {
        private BigDecimal amount;
        private String billSourceId;
        private String goodsName;
        private BigDecimal includeTaxAmount;
        private String includeTaxPrice;
        private Integer lineProperty;
        private String price;
        private String privilegeContent;
        private Integer privilegeFlag;
        private String quantity;
        private String revenueCode;
        private String revenueName;
        private Integer seq;
        private String specification;
        private BigDecimal taxAmount;
        private String taxRate;
        private String units;
        private String zeroTaxRateFlag;
    }

    @Data
    public static class BizControl {
        private String issueBizType;
        private String bizType;
        private BigDecimal monthSurplusLimit;
        private BigDecimal daySurplusLimit;
        private Boolean isWarning;
    }
}
