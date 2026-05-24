package com.invoice.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.invoice.assistant.entity.InvoiceMiddle;
import com.invoice.assistant.entity.InvoiceMiddleDetail;
import com.invoice.assistant.mapper.InvoiceMiddleDetailMapper;
import com.invoice.assistant.mapper.InvoiceMiddleMapper;
import com.invoice.assistant.service.KingdeeInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceMonitorService {

    private final InvoiceMiddleMapper invoiceMiddleMapper;
    private final InvoiceMiddleDetailMapper invoiceMiddleDetailMapper;
    private final KingdeeInvoiceService kingdeeInvoiceService;

    @Value("${monitor.batch-size:50}")
    private int batchSize;

    private void printRecord(InvoiceMiddle record) {
        log.info("========== 单据主表 ==========");
        log.info("  ID          : {}", record.getId());
        log.info("  单据编号    : {}", record.getBillNo());
        log.info("  单据日期    : {}", record.getBillDate());
        log.info("  购买方名称  : {}", record.getBuyerName());
        log.info("  购方税号    : {}", record.getBuyerTaxpayerId());
        log.info("  销方名称    : {}", record.getSellerName());
        log.info("  发票种类    : {}", record.getInvoiceType());
        log.info("  含税标识    : {}", record.getIncludeTaxFlag());
        log.info("  单据金额    : {}", record.getTotalAmount());
        log.info("  开票状态    : {}", record.getIsInvoicing());
        log.info("  备注        : {}", record.getRemark());

        List<InvoiceMiddleDetail> items = invoiceMiddleDetailMapper.findByBillid(record.getId());
        log.info("---------- 明细共 {} 行 ----------", items.size());
        for (int i = 0; i < items.size(); i++) {
            InvoiceMiddleDetail item = items.get(i);
            log.info("  [{}] 商品名称={} 编码={} 数量={} 单价={} 金额={} 税率={} 税额={}",
                    i + 1,
                    item.getGoodsName(), item.getGoodsCode(),
                    item.getQuantity(), item.getPrice(),
                    item.getAmount(), item.getTaxRate(), item.getTaxAmount());
        }
        log.info("==============================");
    }

    @Scheduled(fixedDelayString = "${monitor.poll-interval-seconds:10}000")
    public void pollAndInvoice() {
        List<InvoiceMiddle> pendingList = invoiceMiddleMapper.selectList(
                new LambdaQueryWrapper<InvoiceMiddle>()
                        .eq(InvoiceMiddle::getIsInvoicing, 0)
                        .orderByAsc(InvoiceMiddle::getCreateTime)
                        .last("LIMIT " + batchSize)
        );

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("本次轮询发现 {} 条待开票记录", pendingList.size());

        for (InvoiceMiddle record : pendingList) {
            printRecord(record);
            try {
                kingdeeInvoiceService.doInvoice(record);
            } catch (Exception e) {
                log.error("处理单据 {} 时发生未捕获异常：{}", record.getBillNo(), e.getMessage(), e);
            }
        }
    }
}
