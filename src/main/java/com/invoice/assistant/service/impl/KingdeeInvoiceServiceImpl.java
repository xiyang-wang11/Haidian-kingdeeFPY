package com.invoice.assistant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.invoice.assistant.dto.kingdee.OpenInvoiceRequest;
import com.invoice.assistant.dto.kingdee.OpenInvoiceRequest.BillData;
import com.invoice.assistant.dto.kingdee.OpenInvoiceRequest.BillDetail;
import com.invoice.assistant.dto.kingdee.OpenInvoiceResponse;
import com.invoice.assistant.entity.InvoiceMiddle;
import com.invoice.assistant.entity.InvoiceMiddleDetail;
import com.invoice.assistant.mapper.InvoiceMiddleDetailMapper;
import com.invoice.assistant.mapper.InvoiceMiddleMapper;
import com.invoice.assistant.service.KingdeeAuthService;
import com.invoice.assistant.service.KingdeeInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KingdeeInvoiceServiceImpl implements KingdeeInvoiceService {

    @Value("${kingdee.api.open-invoice-url}")
    private String openInvoiceUrl;

    @Value("${kingdee.api.business-system-code}")
    private String businessSystemCode;

    private final KingdeeAuthService authService;
    private final InvoiceMiddleMapper invoiceMiddleMapper;
    private final InvoiceMiddleDetailMapper detailMapper;
    private final RestTemplate restTemplate;

    @Override
    public void doInvoice(InvoiceMiddle record) {
        // 乐观锁抢占，is_invoicing 0->1，防止并发重复开票
        int locked = invoiceMiddleMapper.lockForInvoicing(record.getId());
        if (locked == 0) {
            log.warn("单据 {} 已被其他线程处理，跳过", record.getBillNo());
            return;
        }

        log.info("开始处理开票，单据号：{}，id：{}", record.getBillNo(), record.getId());

        try {
            List<InvoiceMiddleDetail> details = detailMapper.findByBillid(record.getId());
            OpenInvoiceRequest request = buildRequest(record, details);
            OpenInvoiceResponse response = callKingdeeApi(request);

            if (response != null && response.isSuccess()) {
                invoiceMiddleMapper.markSuccess(
                        record.getId(),
                        response.getInterfaceCode(),
                        response.getReturnCode(),
                        response.getReturnMsg(),
                        response.getInvoiceStatus(),
                        response.getInvoiceCode(),
                        response.getInvoiceNum(),
                        response.getInvoiceDate(),
                        response.getInvoiceFileUrl(),
                        response.getInvoiceImageUrl(),
                        response.getInvoicePdfFileUrl(),
                        response.getInvoiceXmlFileUrl(),
                        response.getOrderNo()
                );
                log.info("开票成功，单据号：{}，发票号码：{}", record.getBillNo(), response.getInvoiceNum());
            } else {
                String reason = response != null ? response.getFailReason() : "响应为空";
                invoiceMiddleMapper.markFailed(record.getId(), response != null ? response.getErrorCode() : "9999", reason);
                log.error("开票失败，单据号：{}，原因：{}", record.getBillNo(), reason);
            }

        } catch (Exception e) {
            log.error("开票异常，单据号：{}，异常：{}", record.getBillNo(), e.getMessage(), e);
            invoiceMiddleMapper.markFailed(record.getId(), "9999", e.getMessage());
        }
    }

    private OpenInvoiceRequest buildRequest(InvoiceMiddle r, List<InvoiceMiddleDetail> details) {
        BillData bill = new BillData();
        bill.setBillNo(r.getBillNo());
        bill.setBillDate(r.getBillDate() != null ? r.getBillDate().toString() : null);
        bill.setTotalAmount(r.getTotalAmount());
        bill.setAutoInvoice(r.getAutoInvoice() != null ? r.getAutoInvoice().toString() : "1");
        bill.setIncludeTaxFlag(r.getIncludeTaxFlag() != null ? r.getIncludeTaxFlag().toString() : null);
        bill.setInvoiceType(r.getInvoiceType());
        bill.setBuyerName(r.getBuyerName());
        bill.setBuyerTaxpayerId(r.getBuyerTaxpayerId());
        bill.setBuyerProperty(r.getBuyerProperty() != null ? r.getBuyerProperty().toString() : null);
        bill.setBuyerRecipientMail(r.getBuyerRecipientMail());
        bill.setBuyerBankAndAccount(r.getBuyerBankAndAccount());
        bill.setBuyerAddressAndTel(r.getBuyerAddressAndTel());
        bill.setSellerName(r.getSellerName());
        bill.setSellerTaxpayerId(r.getSellerTaxpayerId());
        bill.setSellerBankAndAccount(r.getSellerBankAndAccount());
        bill.setSellerAddressAndTel(r.getSellerAddressAndTel());
        bill.setDrawer(r.getDrawer());
        bill.setRemark(r.getRemark());

        List<BillDetail> detailList = details.stream().map(d -> {
            BillDetail bd = new BillDetail();
            bd.setDetailId(d.getId());
            bd.setAmount(d.getAmount());
            bd.setGoodsCode(d.getGoodsCode());
            bd.setGoodsName(d.getGoodsName());
            String lp = d.getLineProperty();
            bd.setLineProperty((lp != null && !"0".equals(lp)) ? Integer.parseInt(lp) : null);
            bd.setRevenueCode(d.getRevenueCode());
            bd.setTaxRate(d.getTaxRate() != null ? d.getTaxRate().toPlainString() : null);
            bd.setQuantity(d.getQuantity());
            bd.setUnits(d.getUnits());
            bd.setSpecification(d.getSpecification());
            bd.setPrice(d.getPrice());
            bd.setTaxAmount(d.getTaxAmount());
            String pf = d.getPrivilegeFlag();
            bd.setPrivilegeFlag((pf != null && !"0".equals(pf)) ? pf : null);
            bd.setPrivilegeContent(d.getPrivilegeContent());
            bd.setDiscountAmount(d.getDiscountAmount());
            bd.setRemark(d.getRemark());
            return bd;
        }).collect(Collectors.toList());

        bill.setBillDetail(detailList);

        // 序列化为 JSON 数组，中文用 Unicode 转义，再 Base64 编码
        String json = JSON.toJSONString(List.of(bill), JSONWriter.Feature.WriteNonStringKeyAsString);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        log.info("开票请求 data 原文：{}", json);

        OpenInvoiceRequest request = new OpenInvoiceRequest();
        request.setBusinessSystemCode(businessSystemCode);
        request.setData(encoded);
        return request;
    }

    private OpenInvoiceResponse callKingdeeApi(OpenInvoiceRequest request) {
        try {
            return doPost(request);
        } catch (TokenExpiredException e) {
            log.warn("Token 已过期，刷新后重试");
            authService.refreshTokens();
            return doPost(request);
        }
    }

    private OpenInvoiceResponse doPost(OpenInvoiceRequest request) {
        String accessToken = authService.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accessToken", accessToken);

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(request), headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(openInvoiceUrl, entity, String.class);

        log.info("金蝶开票接口响应：{}", resp.getBody());
        OpenInvoiceResponse result = JSON.parseObject(resp.getBody(), OpenInvoiceResponse.class);

        if (result != null && "401".equals(result.getReturnCode())) {
            throw new TokenExpiredException("token已过期");
        }
        return result;
    }

    private static class TokenExpiredException extends RuntimeException {
        TokenExpiredException(String msg) { super(msg); }
    }
}
