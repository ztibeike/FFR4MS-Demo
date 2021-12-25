package com.zt.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class ConfController {

    @GetMapping("/getConf")
    public String getConf() {
        String resoult="";
        Map<String,String> zuuMap=new HashMap<String,String>();
        Map<String,String> service=new HashMap<String, String>();
        Map<String, String[]> serviceMap=new HashMap<>();
        RestTemplate restTemplate = new RestTemplate();
        String json = restTemplate.getForObject("http://localhost:8030/eureka/apps", String.class);
        System.out.println(json);
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<String> ArrayIpPort= new ArrayList<>();
        ArrayList<String> ArrayGetWay= new ArrayList<>();
        ArrayList<String> ArrayRelace= new ArrayList<>();
        ArrayList<String> ArrayGroup= new ArrayList<>();
        ArrayList<Boolean> ArrayLeaf= new ArrayList<>();
        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode applicationsNode = rootNode.path("applications").path("application");
            for (int i = 0; i < applicationsNode.size(); i++) {
                String applicationName = applicationsNode.get(i).path("name").asText();
                // map.put(applicationName, new ArrayList<String>());
                if (applicationName.contains("ZUUL")){
                    JsonNode instanceNode = applicationsNode.get(i).path("instance");
                    for (int j = 0; j < instanceNode.size(); j++) {
                        String zuulInfo=instanceNode.get(j).path("ipAddr").asText() + ":" + instanceNode.get(j).path("port").path("$").asText();
                        ArrayGetWay.add(zuulInfo);
                    }
                }else {
                    JsonNode instanceNode = applicationsNode.get(i).path("instance");
                    String[] serArr=new String[instanceNode.size()];
                    for (int j = 0; j < instanceNode.size(); j++) {
                        String serInfo=instanceNode.get(j).path("ipAddr").asText() + ":" + instanceNode.get(j).path("port").path("$").asText();
                        ArrayIpPort.add(serInfo);
                        ArrayGroup.add(applicationName.toLowerCase());
                    }
                }
            }
            String[] a1 = new String[ArrayIpPort.size()];
            ArrayIpPort.toArray(a1);

            String[] a2 = new String[ArrayGetWay.size()];
            ArrayGetWay.toArray(a2);


            String[] a4 = new String[ArrayGroup.size()];
            ArrayGroup.toArray(a4);

            JSONArray jsonarray1 = JSONArray.fromObject(a1);
            System.out.println(jsonarray1);
            JSONArray jsonarray2 = JSONArray.fromObject(a2);
            System.out.println(jsonarray2);

            JSONArray jsonarray4 = JSONArray.fromObject(a4);
            System.out.println(jsonarray4);



            resoult="{\"ArrayIpPort\":"+jsonarray1+",\n" +
                    "\"ArrayGetWay\":"+jsonarray2+",\n" +
                    "\"ArrayGroup\":"+jsonarray4+"}";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resoult;
    }

}