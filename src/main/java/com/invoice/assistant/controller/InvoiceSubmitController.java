package com.invoice.assistant.controller;

import com.invoice.assistant.dto.Result;
import com.invoice.assistant.entity.InvoiceMiddle;
import com.invoice.assistant.entity.InvoiceMiddleDetail;
import com.invoice.assistant.mapper.InvoiceMiddleDetailMapper;
import com.invoice.assistant.mapper.InvoiceMiddleMapper;
import com.invoice.assistant.service.KingdeeInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceSubmitController {

    private final InvoiceMiddleMapper invoiceMiddleMapper;
    private final InvoiceMiddleDetailMapper invoiceMiddleDetailMapper;
    private final KingdeeInvoiceService kingdeeInvoiceService;

    /**
     * 接收海典单据数据，写入中间表后立即触发金蝶开票
     * 请求体为原始 JSON 数组（与海典传入格式一致）
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return Result.fail("数据不能为空");
        }

        int success = 0;
        for (Map<String, Object> data : dataList) {
            try {
                String billId = "SIM" + data.get("billNo");

                // 幂等：已存在则跳过
                if (invoiceMiddleMapper.selectById(billId) != null) {
                    log.warn("单据已存在，跳过插入，billNo={}", data.get("billNo"));
                    continue;
                }

                // 写入主表
                InvoiceMiddle bill = new InvoiceMiddle();
                bill.setId(billId);
                bill.setBillNo(str(data, "billNo"));
                bill.setBillDate(data.get("billDate") != null ? LocalDate.parse(str(data, "billDate")) : null);
                bill.setInvoiceType(str(data, "invoiceType"));
                bill.setBuyerName(str(data, "buyerName"));
                bill.setBuyerTaxpayerId(str(data, "buyerTaxpayerId"));
                bill.setBuyerProperty(intVal(data, "buyerProperty"));
                bill.setBuyerRecipientMail(str(data, "buyerRecipientMail"));
                bill.setSellerName(str(data, "sellerName"));
                bill.setSellerBankAndAccount(str(data, "sellerBankAndAccount"));
                bill.setSellerAddressAndTel(str(data, "sellerAddressAndTel"));
                bill.setTotalAmount(data.get("totalAmount") != null
                        ? new BigDecimal(data.get("totalAmount").toString()) : null);
                bill.setIncludeTaxFlag(intVal(data, "includeTaxFlag"));
                bill.setAutoInvoice(intVal(data, "autoInvoice"));
                bill.setDrawer(str(data, "drawer"));
                bill.setIsInvoicing(0);
                invoiceMiddleMapper.insert(bill);
                log.info("主表写入成功，billNo={}", bill.getBillNo());

                // 写入明细表
                Object detailObj = data.get("billDetail");
                if (detailObj instanceof List) {
                    List<Map<String, Object>> details = (List<Map<String, Object>>) detailObj;
                    for (int i = 0; i < details.size(); i++) {
                        Map<String, Object> d = details.get(i);
                        InvoiceMiddleDetail item = new InvoiceMiddleDetail();
                        item.setId(billId + String.format("%03d", i + 1));
                        item.setBillid(billId);
                        item.setAmount(d.get("amount") != null
                                ? new BigDecimal(d.get("amount").toString()) : null);
                        item.setGoodsCode(str(d, "goodsCode"));
                        item.setLineProperty(d.get("lineProperty") != null
                                ? d.get("lineProperty").toString() : null);
                        item.setRevenueCode(str(d, "revenueCode"));
                        item.setTaxRate(d.get("taxRate") != null
                                ? new BigDecimal(d.get("taxRate").toString()) : null);
                        item.setQuantity(d.get("quantity") != null
                                ? new BigDecimal(d.get("quantity").toString()) : null);
                        item.setUnits(str(d, "units"));
                        invoiceMiddleDetailMapper.insert(item);
                    }
                    log.info("明细写入成功，billNo={}，共{}行", bill.getBillNo(), details.size());
                }

                // 立即触发开票
                log.info("立即触发开票，billNo={}", bill.getBillNo());
                kingdeeInvoiceService.doInvoice(bill);
                success++;

            } catch (Exception e) {
                log.error("处理单据异常：{}，原因：{}", data.get("billNo"), e.getMessage(), e);
                return Result.fail("处理单据 " + data.get("billNo") + " 失败：" + e.getMessage());
            }
        }

        return Result.ok("处理完成，成功 " + success + " 条");
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
}
