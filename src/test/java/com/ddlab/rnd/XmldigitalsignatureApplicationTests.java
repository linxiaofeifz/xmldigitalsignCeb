package com.ddlab.rnd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XmldigitalsignatureApplicationTests {

    @Test
    public void contextLoads() {
        Map<String, String> timeEntry = new HashMap<>();
        timeEntry.put("startTime", "2019-01-01");
        timeEntry.put("endTime", "2019-02-01");
        Map<String, Object> objEntry = new HashMap<>();
        objEntry.put("module", "module");
        objEntry.put("tags", timeEntry);

        Map<String, String> timeEntry1 = new HashMap<>();
        timeEntry1.put("startTime", "2019-01-01");
        timeEntry1.put("endTime", "2019-01-01");
        Map<String, Object> objEntry1 = new HashMap<>();
        objEntry1.put("module", "module1");
        objEntry1.put("tags", timeEntry1);

        Map<String, String> timeEntry2 = new HashMap<>();
        timeEntry2.put("startTime", "2019-01-01");
        timeEntry2.put("endTime", "2019-04-01");
        Map<String, Object> objEntry2 = new HashMap<>();
        objEntry2.put("module", "module2");
        objEntry2.put("tags", timeEntry2);

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(objEntry);
        list.add(objEntry1);
        list.add(objEntry2);

        List<Map<String, Object>> collect = list.stream().sorted((entry, entry1) -> {
            Map<String, String> tags = (Map<String, String>) entry.get("tags");
            Map<String, String> tags1 = (Map<String, String>) entry1.get("tags");
            if (tags.get("endTime").equals(tags1.get("endTime"))) {
                return -1;
            } else {
                return tags.get("endTime").compareTo(tags1.get("endTime"));
            }
        }).collect(Collectors.toList());

        System.out.println(collect);

    }

}
