package com.invoice.assistant.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.invoice.assistant.dto.KingdeeCallbackRequest;
import com.invoice.assistant.dto.KingdeeCallbackRequest.CallbackData;
import com.invoice.assistant.dto.KingdeeCallbackResponse;
import com.invoice.assistant.dto.Result;
import com.invoice.assistant.entity.InvoiceMiddle;
import com.invoice.assistant.mapper.InvoiceMiddleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceCallbackController {

    private final InvoiceMiddleMapper invoiceMiddleMapper;

    /**
     * 金蝶发票云按单回调接口（5.1.03）
     *
     * 一个申请单可能拆分为多张发票，data 数组包含所有发票结果（含成功和失败）。
     * 判断单张发票是否成功：invoiceCode 不为空则成功，否则失败（issueErrorMessage 为失败原因）。
     * 必须按规定格式返回 {"success":true,"code":"0","message":"success"}，否则金蝶会重复回调。
     */
    @PostMapping("/callback")
    public KingdeeCallbackResponse callback(@RequestBody KingdeeCallbackRequest req) {
        log.info("收到金蝶回调，interfaceCode：{}，returnCode：{}，data条数：{}",
                req.getInterfaceCode(), req.getReturnCode(),
                req.getData() != null ? req.getData().size() : 0);

        try {
            if (req.getData() == null || req.getData().isEmpty()) {
                log.warn("回调data为空，interfaceCode：{}", req.getInterfaceCode());
                return KingdeeCallbackResponse.ok();
            }

            for (CallbackData item : req.getData()) {
                processCallbackItem(req.getInterfaceCode(), item);
            }

            return KingdeeCallbackResponse.ok();

        } catch (Exception e) {
            log.error("处理金蝶回调异常：{}", e.getMessage(), e);
            // 即使内部处理异常也返回success，避免金蝶无限重试
            // 异常已记录日志，可人工介入处理
            return KingdeeCallbackResponse.ok();
        }
    }

    /**
     * 查询单据开票状态（供海典系统轮询）
     */
    @GetMapping("/status/{billNo}")
    public Result<InvoiceMiddle> status(@PathVariable String billNo) {
        InvoiceMiddle record = invoiceMiddleMapper.selectOne(
                new LambdaQueryWrapper<InvoiceMiddle>()
                        .eq(InvoiceMiddle::getBillNo, billNo)
                        .last("LIMIT 1")
        );
        if (record == null) {
            return Result.fail("找不到单据：" + billNo);
        }
        return Result.ok(record);
    }

    private void processCallbackItem(String interfaceCode, CallbackData item) {
        String billNo = item.getBillNo();
        log.info("处理回调明细，单据号：{}，发票号码：{}，invoiceCode：{}",
                billNo, item.getInvoiceNum(), item.getInvoiceCode());

        InvoiceMiddle record = invoiceMiddleMapper.selectOne(
                new LambdaQueryWrapper<InvoiceMiddle>()
                        .eq(InvoiceMiddle::getBillNo, billNo)
                        .last("LIMIT 1")
        );

        if (record == null) {
            log.warn("回调找不到对应单据，billNo：{}", billNo);
            return;
        }

        // invoiceCode 不为空表示该张发票开票成功
        boolean success = StringUtils.hasText(item.getInvoiceCode());

        if (success) {
            invoiceMiddleMapper.markSuccess(
                    record.getId(),
                    interfaceCode,
                    "0",
                    "success",
                    item.getInvoiceStatus(),
                    item.getInvoiceCode(),
                    item.getInvoiceNum(),
                    item.getInvoiceDate(),
                    item.getInvoiceFileUrl(),
                    item.getInvoiceImageUrl(),
                    item.getInvoicePdfFileUrl(),
                    item.getInvoiceXmlFileUrl(),
                    item.getOrderNo()
            );
            log.info("回调写入成功，单据号：{}，发票代码：{}，发票号码：{}",
                    billNo, item.getInvoiceCode(), item.getInvoiceNum());
        } else {
            String failReason = StringUtils.hasText(item.getIssueErrorMessage())
                    ? item.getIssueErrorMessage() : "开票失败，无错误信息";
            invoiceMiddleMapper.markFailed(record.getId(), "9999", failReason);
            log.warn("回调写入失败，单据号：{}，原因：{}", billNo, failReason);
            println("发票助手启动成功！");
        }
    }
}
