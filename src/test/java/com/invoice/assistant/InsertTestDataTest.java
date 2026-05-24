package com.invoice.assistant;

import com.invoice.assistant.entity.InvoiceMiddle;
import com.invoice.assistant.entity.InvoiceMiddleDetail;
import com.invoice.assistant.mapper.InvoiceMiddleDetailMapper;
import com.invoice.assistant.mapper.InvoiceMiddleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
public class InsertTestDataTest {

    @Autowired
    private InvoiceMiddleMapper invoiceMiddleMapper;

    @Autowired
    private InvoiceMiddleDetailMapper invoiceMiddleDetailMapper;

    @Test
    public void insertTestData() {
        String billId = "SIM20260524000009";

        InvoiceMiddle bill = new InvoiceMiddle();
        bill.setId(billId);
        bill.setBillNo("20260524000009");
        bill.setBillDate(LocalDate.of(2026, 1, 28));
        bill.setInvoiceType("10xdp");
        bill.setBuyerName("金蝶票据云科技(深圳)有限公司");
        bill.setBuyerTaxpayerId("91440300MA5G9GK78Y");
        bill.setBuyerProperty(0);
        bill.setBuyerRecipientMail("18507152980@163.com");
        bill.setSellerName("金蝶软件（中国）有限公司");
        bill.setSellerBankAndAccount("13710884704");
        bill.setSellerAddressAndTel("高新技术产业园南区科技南十二路58996989");
        bill.setTotalAmount(new BigDecimal("100.00"));
        bill.setIncludeTaxFlag(0);
        bill.setAutoInvoice(0);
        bill.setDrawer("王协芬");
        bill.setIsInvoicing(0);

        invoiceMiddleMapper.insert(bill);
        System.out.println("主表插入成功，id=" + billId);

        InvoiceMiddleDetail item = new InvoiceMiddleDetail();
        item.setId("ITEM20260524000009001");
        item.setBillid(billId);
        item.setAmount(new BigDecimal("100.00"));
        item.setGoodsCode("KPX20250821000258");
        item.setLineProperty("2");
        item.setRevenueCode("3070301000000000000");
        item.setTaxRate(new BigDecimal("0.06"));
        item.setQuantity(new BigDecimal("1"));
        item.setUnits("千克");

        invoiceMiddleDetailMapper.insert(item);
        System.out.println("明细表插入成功，id=" + item.getId());
    }

    /**
     * 按海典推送的 base64 数据格式写入中间表，用于测试监听+开票流程。
     * 数据来源（解码后）：
     *   billNo=20260524000009, buyerName=金蝶票据云科技(深圳)有限公司
     *   明细 detailId=202410080001032, goodsCode=KPX20250821000258
     */
    @Test
    public void insertFromBase64Data() {
        // 主表 id = "SIM" + billNo，与 InvoiceSubmitController 保持一致
        String billNo = "20260524000013";
        String billId = "SIM" + billNo;

        // 幂等：已存在则跳过
        if (invoiceMiddleMapper.selectById(billId) != null) {
            System.out.println("主表记录已存在，跳过，id=" + billId);
            return;
        }

        InvoiceMiddle bill = new InvoiceMiddle();
        bill.setId(billId);
        bill.setBillNo(billNo);
        bill.setBillDate(LocalDate.of(2026, 1, 28));
        bill.setTotalAmount(new BigDecimal("100"));
        bill.setAutoInvoice(1);
        bill.setIncludeTaxFlag(0);
        bill.setInvoiceType("10xdp");
        bill.setBuyerName("金蝶票据云科技(深圳)有限公司");
        bill.setBuyerTaxpayerId("91440300MA5G9GK78Y");
        bill.setBuyerProperty(0);
        bill.setBuyerRecipientMail("18507152980@163.com");
        bill.setSellerName("金蝶软件（中国）有限公司");
        bill.setSellerBankAndAccount("915003006188392540");
        bill.setSellerAddressAndTel("高新技术产业园南区科技南十二路58996989");
        bill.setDrawer("王协芬");
        bill.setIsInvoicing(0);

        invoiceMiddleMapper.insert(bill);
        System.out.println("主表插入成功，id=" + billId);

        // 明细 id = detailId（来自海典数据），billid 关联主表
        InvoiceMiddleDetail item = new InvoiceMiddleDetail();
        item.setId("202410080001036");
        item.setBillid(billId);
        item.setAmount(new BigDecimal("100"));
        item.setGoodsCode("KPX20250821000258");
        item.setGoodsName("测试商品");
        item.setLineProperty("2");
        item.setRevenueCode("3070301000000000000");
        item.setTaxRate(new BigDecimal("0.06"));
        item.setQuantity(new BigDecimal("1"));
        item.setUnits("千克");

        invoiceMiddleDetailMapper.insert(item);
        System.out.println("明细表插入成功，id=" + item.getId());
    }
}
