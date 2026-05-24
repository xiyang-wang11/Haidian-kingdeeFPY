import java.sql.*;

public class InsertData {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/invoice_system?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("数据库连接成功");

            String billId = "SIM20260524000088";

            // 检查是否已存在
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM t_sim_original_bill WHERE id=?")) {
                ps.setString(1, billId);
                ResultSet rs = ps.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("记录已存在，跳过插入");
                    return;
                }
            }

            // 插入主表（列名使用数据库实际驼峰格式）
            String sql = "INSERT INTO t_sim_original_bill " +
                "(id, billNo, billDate, invoiceType, buyerName, buyerTaxpayerId, buyerProperty, " +
                "buyerRecipientMail, sellerName, sellerBankAndAccount, sellerAddressAndTel, " +
                "totalAmount, includeTaxFlag, autoInvoice, drawer, IsInvoicing) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, billId);
                ps.setString(2, "20260524000088");
                ps.setString(3, "2026-01-28");
                ps.setString(4, "10xdp");
                ps.setString(5, "金蝶票据云科技(深圳)有限公司");
                ps.setString(6, "91440300MA5G9GK78Y");
                ps.setInt(7, 0);
                ps.setString(8, "18507152980@163.com");
                ps.setString(9, "金蝶软件（中国）有限公司");
                ps.setString(10, "13710884704");
                ps.setString(11, "高新技术产业园南区科技南十二路58996989");
                ps.setBigDecimal(12, new java.math.BigDecimal("100.00"));
                ps.setInt(13, 0);
                ps.setInt(14, 0);
                ps.setString(15, "王协芬");
                ps.setInt(16, 0);
                int rows = ps.executeUpdate();
                System.out.println("主表插入成功，影响行数=" + rows);
            }

            // 插入明细表（列名使用数据库实际驼峰格式）
            String sqlItem = "INSERT INTO t_sim_original_bill_item " +
                "(id, billid, amount, goodsCode, lineProperty, revenueCode, taxRate, quantity, units) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                ps.setString(1, "ITEM20260524000088001");
                ps.setString(2, billId);
                ps.setBigDecimal(3, new java.math.BigDecimal("100.00"));
                ps.setString(4, "KPX20250821000258");
                ps.setString(5, "2");
                ps.setString(6, "3070301000000000000");
                ps.setBigDecimal(7, new java.math.BigDecimal("0.06"));
                ps.setBigDecimal(8, new java.math.BigDecimal("1"));
                ps.setString(9, "千克");
                int rows = ps.executeUpdate();
                System.out.println("明细表插入成功，影响行数=" + rows);
            }

            System.out.println("全部插入完成，监听程序将在下次轮询时自动触发开票");
        }
    }
}
