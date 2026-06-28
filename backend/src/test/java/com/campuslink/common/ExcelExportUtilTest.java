package com.campuslink.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ExcelExportUtil} Excel 导出工具单元测试。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class ExcelExportUtilTest {

    @Test
    @DisplayName("writeXlsx 输出合法的 xlsx 字节流与下载头")
    void writeXlsx() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of(1, "张三", 95.5));
        rows.add(List.of(2, "李四", 88));

        ExcelExportUtil.writeXlsx(response, "demo",
                List.of("ID", "姓名", "分数"), rows);

        byte[] bytes = response.getContentAsByteArray();
        assertTrue(bytes.length > 0, "导出内容不应为空");
        // xlsx 本质是 zip，魔数 PK (0x50 0x4B)
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("attachment"));
    }

    @Test
    @DisplayName("空数据行也能导出表头")
    void writeXlsxEmptyRows() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelExportUtil.writeXlsx(response, "empty",
                List.of("列1", "列2"), new ArrayList<>());
        assertTrue(response.getContentAsByteArray().length > 0);
    }
}
