package com.invoice.assistant.dto;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** 金蝶发票云按单回调请求体（5.1.03） */
@Data
public class KingdeeCallbackRequest {

    /** 业务编码：INVOICE.OPEN-开票，INVOICE.CANCEL-作废，INVOICE.RED-红冲 */
    private String interfaceCode;

    /** 返回编码：0-成功，9999-失败 */
    private String returnCode;

    /** 返回信息 */
    private String returnMsg;

    /**
     * 回调数据：金蝶可能传 JSON 数组对象，也可能传 base64 编码的字符串。
     * 统一用 Object 接收，调用 decodeData() 获取解析后的列表。
     */
    private Object data;

    /** 业务管控信息 */
    private BizControl bizControl;

    /**
     * 兼容两种 data 格式：
     * 1. JSON 数组（Jackson 反序列化为 List<LinkedHashMap>）
     * 2. base64 字符串（解码后再解析为 JSON 数组）
     */
    public List<CallbackData> decodeData() {
        if (data == null) {
            return Collections.emptyList();
        }
        String json;
        if (data instanceof String) {
            String str = (String) data;
            try {
                // 尝试 base64 解码
                byte[] decoded = Base64.getDecoder().decode(str);
                json = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // 不是 base64，直接当 JSON 字符串处理
                json = str;
            }
        } else {
            // 已经是对象/数组，转回 JSON 字符串再统一解析
            json = JSON.toJSONString(data);
        }
        return JSON.parseObject(json, new TypeReference<List<CallbackData>>() {});
    }

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
        /** 发票代码（全电发票为空，以 invoiceNum 不为空判断成功） */
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
