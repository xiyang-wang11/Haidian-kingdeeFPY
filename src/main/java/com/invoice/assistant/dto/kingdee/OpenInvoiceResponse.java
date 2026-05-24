package com.invoice.assistant.dto.kingdee;

import lombok.Data;

@Data
public class OpenInvoiceResponse {
    private String errorCode;
    private String message;
    private Boolean status;
    private Boolean success;

    /** 业务编码，开票回调是INVOICE.OPEN */
    private String interfaceCode;
    /** 返回编码：0-成功，9999-失败 */
    private String returnCode;
    /** 返回信息，成功返回success，失败返回原因 */
    private String returnMsg;
    /** 发票状态：0-正常，2-部分红冲，3-红冲，6-作废 */
    private String invoiceStatus;
    /** 发票代码 */
    private String invoiceCode;
    /** 发票号码 */
    private String invoiceNum;
    /** 开票日期 */
    private String invoiceDate;
    /** 版式文件下载地址（数电票为ofd） */
    private String invoiceFileUrl;
    /** PDF转图片预览地址 */
    private String invoiceImageUrl;
    /** 数电票PDF地址 */
    private String invoicePdfFileUrl;
    /** 数电票XML地址 */
    private String invoiceXmlFileUrl;
    /** 发票流水号 */
    private String orderNo;

    public boolean isSuccess() {
        return Boolean.TRUE.equals(status) || Boolean.TRUE.equals(success);
    }

    public String getFailReason() {
        return message != null ? message : errorCode;
    }
}
