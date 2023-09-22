package com.taogen.datahandling.facade.es;

import com.taogen.commons.collection.CollectionUtils;
import com.taogen.commons.datatypes.datetime.DateRangeUtils;
import com.taogen.commons.io.DirectoryUtils;
import com.taogen.datahandling.common.vo.LabelAndData;
import com.taogen.datahandling.es.service.EsReader;
import com.taogen.datahandling.es.vo.DslQueryParam;
import com.taogen.datahandling.mysql.service.MySQLReader;
import com.taogen.datahandling.office.excel.service.service.ExcelWriter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.taogen.datahandling.facade.es.EsFieldInfo.*;

/**
 * @author taogen
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
@Disabled
class YuqingDataExportTest {
    @Autowired(required = true)
    private EsReader esReader;
    @Autowired(required = true)
    private RestClient restClient;
    @Autowired(required = true)
    private MySQLReader mySQLReader;
    @Autowired(required = true)
    private JdbcTemplate jdbcTemplate;
    @Autowired(required = true)
    private ExcelWriter excelWriter;

    @Test
    void test() {
//        List<EsFieldInfo> queryFields = Arrays.asList(TITLE, CONTENT, AUTHOR, PUB_TIME, SOURCE_URL, HOST_NAME, IP_REGION, SOURCE_NAME, CHECK_IN_AREA, LEVEL_NAME);
//        String dsl = "{\n" +
//                "    \"query\": {\n" +
//                "        \"bool\": {\n" +
//                "            \"must\": {\n" +
//                "              \"range\": {\"pub_time\": {\"gte\": \"2023-09-01 00:10:00\",\"lte\": \"2023-09-01 00:11:00\"}}\n" +
//                "          }\n" +
//                "        }\n" +
//                "    }\n" +
//                "}";
//        JSONObject jsonObject = new JSONObject(dsl);
//        jsonObject.put("size", 50);
//        jsonObject.put("sort", Collections.singletonList(Collections.singletonMap(
//                "pub_time", Collections.singletonMap("order", "desc"))));
//        List<String> fetchFields = queryFields.stream().map(EsFieldInfo::getQueryField).distinct().collect(Collectors.toList());
//        jsonObject.put("_source", fetchFields);
//        dsl = jsonObject.toString();
//        log.debug("dsl: {}", dsl);

        List<EsFieldInfo> queryFields = Arrays.asList(EsFieldInfo.values());
        String keywordExpression = "";
        Map<EsFieldInfo, Object[]> queryConditions = Stream.of(new Object[][]{
                {DEP, new Object[]{"1", "2"}},
                {SOURCE_NAME, new Object[]{SourceType.NEWS.getSourceId(), SourceType.VIDEO.getSourceId()}},
                {PUB_TIME, new Object[]{"2023-09-01 00:10:00", "2023-09-01 00:11:00"}},
                {LEVEL_NAME, new Object[]{SensitiveType.SENSITIVE.getLevelId()}},
                {HOST_NAME, new Object[]{"baidu.com"}},
                {AUTHOR, new Object[]{"zhangsan"}},
        }).collect(Collectors.toMap(data -> (EsFieldInfo) data[0], data -> (Object[]) data[1]));

        String dsl = buildDslStr(queryFields, queryConditions, keywordExpression, 50);

    }

    private String buildDslStr(List<EsFieldInfo> queryFields,
                               Map<EsFieldInfo, Object[]> queryConditions,
                               String keywordExpression,
                               Integer size,
                               String orderField,
                               SortOrder sortOrder) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (queryFields != null) {
            List<String> fetchFields = queryFields.stream().map(EsFieldInfo::getQueryField).distinct().collect(Collectors.toList());
            searchSourceBuilder.fetchSource(fetchFields.toArray(new String[0]), null);
        }
        if (size != null) {
            searchSourceBuilder.size(size);
        }
        if (orderField != null && sortOrder != null) {
            searchSourceBuilder.sort(orderField, sortOrder);
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 非垃圾数据（默认条件，和舆情搜索保持一致）
        boolQueryBuilder.must(QueryBuilders.termQuery(EsField.STATUS, "0"));
        BoolQueryBuilder keywordBoolQuery = KeywordBuilder.getBoolQueryBuilderByExpression(keywordExpression);
        if (keywordBoolQuery != null) {
            boolQueryBuilder.must(keywordBoolQuery);
        }
        if (queryConditions != null) {
            for (Map.Entry<EsFieldInfo, Object[]> entry : queryConditions.entrySet()) {
                EsFieldInfo esFieldInfo = entry.getKey();
                Object[] values = entry.getValue();
                boolQueryBuilder.must(esFieldInfo.getQueryFunction().apply(values));
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        log.debug("searchSourceBuilder: {}", searchSourceBuilder);
        return searchSourceBuilder.toString();
    }

    @Test
    void exportDataToExcel() throws IOException, ParseException {
//        String startDate = "2023-09-01";
//        String endDate = "2023-09-01";
//        List<EsFieldInfo> queryFields = Arrays.asList(TITLE, CONTENT, AUTHOR, PUB_TIME, SOURCE_URL, HOST_NAME, IP_REGION, SOURCE_NAME, CHECK_IN_AREA, LEVEL_NAME);
////        List<EsField> queryFields = Arrays.asList(ID, PUB_TIME);
//        String dsl = "{\n" +
//                "    \"query\": {\n" +
//                "        \"bool\": {\n" +
//                "            \"must\": {\n" +
//                "              \"range\": {\"pub_time\": {\"gte\": \"2023-09-01 00:10:00\",\"lte\": \"2023-09-01 00:11:00\"}}\n" +
//                "          }\n" +
//                "        }\n" +
//                "    }\n" +
//                "}";
//        JSONObject jsonObject = new JSONObject(dsl);
//        jsonObject.put("size", 50);
//        jsonObject.put("sort", Collections.singletonList(Collections.singletonMap(
//                "pub_time", Collections.singletonMap("order", "desc"))));
//        jsonObject.put("_source", queryFields.stream().map(EsFieldInfo::getQueryField).distinct().collect(Collectors.toList()));
//        dsl = jsonObject.toString();

        String startDate = "2023-09-01";
        String endDate = "2023-09-01";

        int size = 50;
        List<EsFieldInfo> queryFields = Arrays.asList(EsFieldInfo.values());
        String keywordExpression = "";
        Map<EsFieldInfo, Object[]> queryConditions = Stream.of(new Object[][]{
                {DEP, new Object[]{"1", "2"}},
                {SOURCE_NAME, new Object[]{SourceType.NEWS.getSourceId(), SourceType.VIDEO.getSourceId()}},
                {PUB_TIME, new Object[]{"2023-09-01 00:10:00", "2023-09-01 00:11:00"}},
                {LEVEL_NAME, new Object[]{SensitiveType.SENSITIVE.getLevelId()}},
                {HOST_NAME, new Object[]{"baidu.com"}},
                {AUTHOR, new Object[]{"zhangsan"}},
        }).collect(Collectors.toMap(data -> (EsFieldInfo) data[0], data -> (Object[]) data[1]));
        String dsl = buildDslStr(queryFields, queryConditions, keywordExpression, size);
        log.debug("dsl: {}", dsl);

        DslQueryParam dslQueryParam = new DslQueryParam();
        List<String> indexNames = getIndexNames(startDate, endDate);
        log.debug("indexNames: {}", indexNames);
        dslQueryParam.setIndex(indexNames);
        dslQueryParam.setDsl(dsl);
        List<JSONObject> jsonObjectList = esReader.readAll(restClient, dslQueryParam);
        if (jsonObjectList == null) {
            log.warn("No data to export!");
            System.exit(0);
        }
        log.debug("first data: {}", jsonObjectList.get(0));
        addTranslateField(jsonObjectList, queryFields);
        LabelAndData labelAndData = convertToLabelAndData(jsonObjectList, queryFields);
        if (labelAndData == null) {
            log.warn("No data to export!");
            System.exit(0);
        }
        String outputDir = DirectoryUtils.getUserHomeDir() + File.separator + "export";
        String outputFileName = new StringBuilder()
                .append("舆情-数据-")
                .append(System.currentTimeMillis())
                .append(".xlsx")
                .toString();
        String outputFilePath = outputDir + File.separator + outputFileName;
        log.debug("prepared to export data to excel...");
        log.debug("outputFilePath: {}", outputFilePath);
        excelWriter.writeLabelAndDataToExcel(labelAndData, outputFilePath);
        log.debug("export success!");
        log.debug("outputFilePath: {}", outputFilePath);
    }

    private LabelAndData convertToLabelAndData(List<JSONObject> jsonObjectList, List<EsFieldInfo> queryFields) {
        if (CollectionUtils.isEmpty(jsonObjectList) || CollectionUtils.isEmpty(queryFields)) {
            return null;
        }
        LabelAndData labelAndData = new LabelAndData();
        labelAndData.setLabels(queryFields.stream().map(EsFieldInfo::getLabelName).collect(Collectors.toList()));
        List<List<Object>> valuesList = new ArrayList<>();
        jsonObjectList.forEach(jsonObject -> {
            List<Object> values = new ArrayList<>();
            queryFields.forEach(field -> {
                try {
                    values.add(jsonObject.get(field.getExportField()));
                } catch (Exception e) {
                    log.warn("No field {} in jsonObject {}", field.getExportField(), jsonObject);
                    values.add(null);
                }
            });
            valuesList.add(values);
        });
        labelAndData.setValuesList(valuesList);
        return labelAndData;
    }

    /**
     * Add translate field to jsonObjectList
     * Don't add null value to jsonObjectList
     *
     * @param jsonObjectList
     * @param queryFields
     */
    private void addTranslateField(List<JSONObject> jsonObjectList, List<EsFieldInfo> queryFields) {
        if (CollectionUtils.isEmpty(jsonObjectList) || CollectionUtils.isEmpty(queryFields)) {
            return;
        }
        // addHostName
        if (queryFields.contains(HOST_NAME)) {
            List<String> hosts = jsonObjectList.stream().filter(item -> item.has("host")).map(item -> item.getString("host"))
                    .distinct().collect(Collectors.toList());
            log.debug("host size: {}", hosts.size());
            String placeholder = String.join(", ", Collections.nCopies(hosts.size(), "?"));
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList("select host, name from dict_site_info where host in (" + placeholder + ")", hosts.toArray());
            Map<String, String> hostNameMap = mapList.stream().collect(Collectors.toMap(item -> (String) item.get("host"), item -> (String) item.get("name")));
            jsonObjectList.stream().filter(item -> item.has("host"))
                    .forEach(item -> {
                        String hostName = hostNameMap.get(item.getString("host"));
                        if (StringUtils.isNotBlank(hostName)) {
                            item.put(HOST_NAME.getExportField(), hostName);
                        } else {
                            item.put(HOST_NAME.getExportField(), item.getString("host"));
                        }
                    });
        }
        // addSourceName
        if (queryFields.contains(SOURCE_NAME)) {
            jsonObjectList.stream().filter(item -> item.has("source_id"))
                    .forEach(item -> item.put(SOURCE_NAME.getExportField(),
                            SourceType.fromSourceId(item.getString("source_id")).getSourceName()));
        }
        // addLevelName
        if (queryFields.contains(LEVEL_NAME)) {
            jsonObjectList.stream().filter(item -> item.has("level_id"))
                    .forEach(item -> item.put(LEVEL_NAME.getExportField(),
                            SensitiveType.fromLevelId(item.getString("level_id")).getLevelName()));
        }
        // addIpArea
        if (queryFields.contains(IP_REGION)) {
            final Pattern IP_AREA_PATTERN = Pattern.compile("\"ip_region\":\\[(.*?)\\]");
            jsonObjectList.stream().filter(item -> item.has("remark1"))
                    .forEach(item -> {
                        String remark1 = item.getString("remark1");
                        String ipAreaStr = "";
                        if (StringUtils.isNotBlank(remark1)) {
                            Matcher matcher = IP_AREA_PATTERN.matcher(remark1);
                            while (matcher.find()) {
                                String ipArea = matcher.group(1);
                                ipAreaStr = Arrays.stream(ipArea.split(","))
                                        .map(area -> area.trim().replace("\"", ""))
                                        .collect(Collectors.joining(""));
                            }
                        }
                        if (ipAreaStr == null) {
                            ipAreaStr = "";
                        }
                        item.put(IP_REGION.getExportField(), ipAreaStr);
                    });
        }

        // addCheckInArea
        if (queryFields.contains(CHECK_IN_AREA)) {
            jsonObjectList.stream().filter(item -> item.has("content"))
                    .forEach(item -> {
                        String content = item.getString("content");
                        String key = "签到地点：";
                        int index = content.lastIndexOf(key);
                        if (index != -1) {
                            item.put(CHECK_IN_AREA.getExportField(), content.substring(index + key.length()));
                        } else {
                            item.put(CHECK_IN_AREA.getExportField(), "");
                        }
                    });
        }
    }

    private List<String> getIndexNames(String startDate, String endDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<String> dates = DateRangeUtils.getDateStringsBetweenDates(
                dateFormat.parse(startDate), dateFormat.parse(endDate), "yyyyMMdd");
        if (CollectionUtils.isEmpty(dates)) {
            log.warn("No date between {} and {}", startDate, endDate);
            System.exit(0);
        }
        List<String> indexes = dates.stream().map(item -> "alias-meta-" + item).collect(Collectors.toList());
        return indexes;
    }

}