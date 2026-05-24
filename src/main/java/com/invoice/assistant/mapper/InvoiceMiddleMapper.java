package com.invoice.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.invoice.assistant.entity.InvoiceMiddle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InvoiceMiddleMapper extends BaseMapper<InvoiceMiddle> {

    /** 乐观锁抢占，IsInvoicing 0->1，防止并发重复开票 */
    @Update("UPDATE t_sim_original_bill SET IsInvoicing=1, updateTime=NOW() WHERE id=#{id} AND IsInvoicing=0")
    int lockForInvoicing(@Param("id") String id);

    /** 开票成功，写入返回字段，IsInvoicing=1 */
    @Update("UPDATE t_sim_original_bill SET IsInvoicing=1, " +
            "interfaceCode=#{interfaceCode}, returnCode=#{returnCode}, returnMsg=#{returnMsg}, " +
            "invoiceStatus=#{invoiceStatus}, invoiceCode=#{invoiceCode}, invoiceNum=#{invoiceNum}, " +
            "invoiceDate=#{invoiceDate}, invoiceFileUrl=#{invoiceFileUrl}, invoiceImageUrl=#{invoiceImageUrl}, " +
            "invoicePdfFileUrl=#{invoicePdfFileUrl}, invoiceXmlFileUrl=#{invoiceXmlFileUrl}, " +
            "orderNo=#{orderNo}, updateTime=NOW() WHERE id=#{id}")
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

    /** 开票失败，IsInvoicing=2 */
    @Update("UPDATE t_sim_original_bill SET IsInvoicing=2, " +
            "returnCode=#{returnCode}, returnMsg=#{returnMsg}, updateTime=NOW() WHERE id=#{id}")
    int markFailed(@Param("id") String id,
                   @Param("returnCode") String returnCode,
                   @Param("returnMsg") String returnMsg);
}
