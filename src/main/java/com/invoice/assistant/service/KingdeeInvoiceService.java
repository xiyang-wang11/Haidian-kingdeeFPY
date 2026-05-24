package com.invoice.assistant.service;

import com.invoice.assistant.entity.InvoiceMiddle;

public interface KingdeeInvoiceService {

    /** 对单条中间表记录执行开票 */
    void doInvoice(InvoiceMiddle record);
}
