package com.invoice.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.invoice.assistant.entity.InvoiceMiddleDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InvoiceMiddleDetailMapper extends BaseMapper<InvoiceMiddleDetail> {

    @Select("SELECT * FROM t_sim_original_bill_item WHERE billid=#{billid}")
    List<InvoiceMiddleDetail> findByBillid(@Param("billid") String billid);
}
