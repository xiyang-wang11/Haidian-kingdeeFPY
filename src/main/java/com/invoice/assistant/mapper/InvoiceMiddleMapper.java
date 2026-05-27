package com.invoice.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.invoice.assistant.entity.InvoiceMiddle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InvoiceMiddleMapper extends BaseMapper<InvoiceMiddle> {

    /** 乐观锁抢占，is_invoicing 0->1，防止并发重复开票 */
    @Update("UPDATE t_sim_original_bill SET is_invoicing=1, update_time=NOW() WHERE id=#{id} AND is_invoicing=0")
    int lockForInvoicing(@Param("id") String id);

    /** 开票成功，写入返回字段，is_invoicing=1 */
    @Update("UPDATE t_sim_original_bill SET is_invoicing=1, " +
            "interface_code=#{interfaceCode}, return_code=#{returnCode}, return_msg=#{returnMsg}, " +
            "invoice_status=#{invoiceStatus}, invoice_code=#{invoiceCode}, invoice_num=#{invoiceNum}, " +
            "invoice_date=#{invoiceDate}, invoice_file_url=#{invoiceFileUrl}, invoice_image_url=#{invoiceImageUrl}, " +
            "invoice_pdf_file_url=#{invoicePdfFileUrl}, invoice_xml_file_url=#{invoiceXmlFileUrl}, " +
            "order_no=#{orderNo}, update_time=NOW() WHERE id=#{id}")
    int markSuccess(@Param("id") String id,
                    @Param("interfaceCode") String interfaceCode,
                    @Param("returnCode") String returnCode,
                    @Param("returnMsg") String returnMsg,
                    @Param("invoiceStatus") String invoiceStatus,
                    @Param("invoiceCode") String invoiceCode,
                    @Param("invoiceNum") String invoiceNum,
                    @Param("invoiceDate") String invoiceDate,
                    @Param("invoiceFileUrl") String invoiceFileUrl,
                    @Param("invoiceImageUrl") String invoiceImageUrl,
                    @Param("invoicePdfFileUrl") String invoicePdfFileUrl,
                    @Param("invoiceXmlFileUrl") String invoiceXmlFileUrl,
                    @Param("orderNo") String orderNo);

    /** 开票失败，is_invoicing=2 */
    @Update("UPDATE t_sim_original_bill SET is_invoicing=2, " +
            "return_code=#{returnCode}, return_msg=#{returnMsg}, update_time=NOW() WHERE id=#{id}")
    int markFailed(@Param("id") String id,
                   @Param("returnCode") String returnCode,
                   @Param("returnMsg") String returnMsg);
}
