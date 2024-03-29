package com.taogen.datahandling.facade.mysql.examine;

import com.taogen.commons.io.FileUtils;
import com.taogen.datahandling.common.vo.LabelAndData;
import com.taogen.datahandling.facade.base.ExportBaseTest;
import com.taogen.datahandling.mysql.vo.SqlQueryParam;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Map;

/**
 * @author Taogen
 */
@Slf4j
@Disabled
class ExamineDepositRecordExportTest extends ExportBaseTest {

    @BeforeEach
    void beforeEach() {
        showConfig();
    }

    @ParameterizedTest
    @CsvSource({
            "214, 2023-09-01 00:00:00, 2023-09-31 23:59:59",
            "267, 2023-09-01 00:00:00, 2023-09-31 23:59:59",
            "485, 2023-09-01 00:00:00, 2023-09-31 23:59:59"
    })
    void exportExamineDeposit(int deptId, String beginTime, String endTime) throws IOException {
        String sql = "select dept.dept_name as \"客户名称\", user.user_name \"账号名称\", count(*) \"消费次数\", SUM(LENGTH) as \"消费字数\"\n" +
                "from examine.examine_deposit_record as deposit \n" +
                "left join examine.sys_dept as dept on deposit.dept_id = dept.dept_id\n" +
                "left join examine.sys_user as user on deposit.create_by = user.user_id\n" +
                "WHERE\n" +
                "deposit.type in (1, 2, 5) and /*1在线审核，2文档审核，5智能改写*/\n" +
                "deposit.dept_id=? and \n" +
                "deposit.`length` < 0 and \n" +
                "deposit.create_time between ? and ?\n" +
                "group by deposit.create_by";
        String selectDeptNameSql = "select dept_name from examine.sys_dept where dept_id=?";
        Map<String, Object> deptNameMap = jdbcTemplate.queryForMap(selectDeptNameSql, deptId);
        String deptName = deptNameMap.get("dept_name").toString();
        SqlQueryParam sqlQueryParam = new SqlQueryParam();
        sqlQueryParam.setSql(sql);
        sqlQueryParam.setArgs(new Object[]{deptId, beginTime, endTime});
        LabelAndData labelAndData = mysqlReader.read(jdbcTemplate, sqlQueryParam);
        String outputPath = getExportDirPath() + FileUtils.appendDateTimeToFileName(deptName + ".xlsx");
        outputPath = FileUtils.appendDateTimeToFileName(outputPath);
        log.debug("outputPath is: {}", outputPath);
        excelWriter.writeLabelAndDataToExcel(labelAndData, outputPath);
    }
}
