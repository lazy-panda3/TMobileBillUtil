package com.prasanth.namana.tmobile.Sevice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Scope(value="prototype", proxyMode= ScopedProxyMode.TARGET_CLASS)
@Service
public class UnploadServiceImpl implements UploadService{

    private final String[] headers= {"Plan Cost","Equipment Cost","Services Cost", "One-Time Charges","Total"};
    private final String[] manditory = {"Account", "Totals"};
    private Map<String, Double[]> individualTotal = new HashMap<>();
    private int totalLines = 0;
    private double commonCharges = 0;
    private DecimalFormat df = new DecimalFormat("#.##");
    private Double[] calculatedTotal = {0.0,0.0,0.0,0.0,0.0};
    private final String  accountRegEx  = "([(]{1}[0-9]{3}[)]{1}[\\s]?[0-9]{3}[-]{1}[0-9]{4}|[a-zA-Z]+[\\s]+)(.*)";
    private String format  = "<tr><td>%1$s</td><td>%2$s</td><td>%3$s</td><td>%4$s</td><td>%5$s</td><td>%6$s</td></tr>";

    @Override
    public String processFile(MultipartFile multipartFile) throws IOException {
        try {
            if(!multipartFile.getOriginalFilename().endsWith("pdf")) return "Nice Try! Upload PDF file ";
            File temp = new File(multipartFile.getOriginalFilename());
            if (temp.createNewFile()) {
                System.out.println("File created: " + temp.getName());
            } else {
                System.out.println("File already exists.");
            }
            byte[] bytes = multipartFile.getBytes();
            FileOutputStream fos = new FileOutputStream(temp);
            fos.write(bytes);
            fos.close();

            PDDocument document  = PDDocument.load(temp);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String textString = pdfStripper.getText(document);
            if(!textString.contains("T-Mobile")) return " Please Upload T-mobile Bill PDF";
            String[] text = textString.split(pdfStripper.getLineSeparator());
            String finalSplit = this.preProcessData(text,pdfStripper.getWordSeparator());
            Matrix textLineMatrix = pdfStripper.getTextLineMatrix();

            temp.delete();
            return formatResult();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return "";
    }

    private String formatResult() {
        StringBuilder result = new StringBuilder();
        result.append("<table>");
        result.append("<tr><th></th>");
        for(String header:headers)
            result.append("<th>"+header+"</th>");
        result.append("</tr>");

        String temp = "";
        Double[] chargesPerPerson;
        for(String key : individualTotal.keySet()){
            chargesPerPerson = individualTotal.get(key);
            if(key.equals(manditory[0]))continue;
            if(key.matches("^[^A-Za-z]+.*"))
                result.append(String.format(format,key,chargesPerPerson[0],chargesPerPerson[1],chargesPerPerson[2],
                        chargesPerPerson[3],df.format(chargesPerPerson[4])));
            else
                temp += String.format(format,key,chargesPerPerson[0],chargesPerPerson[1],chargesPerPerson[2],
                        chargesPerPerson[3],df.format(chargesPerPerson[4]));
        }
        result.append(temp);
        result.append("</table>");
        return result.toString();

    }

    private String preProcessData(String[] text, String wordSeperator) {
        List<String> formattedData = formatData(text);
        Pattern accountPattern  = Pattern.compile(accountRegEx);
        Matcher matcher = null;
        for(String line : formattedData) {
            matcher = accountPattern.matcher(line);
            if(!matcher.find())continue;
            extractData(matcher.group(1).trim(),matcher.group(2));
        }
        System.out.println(totalLines);
        calculateTotals();
        return null;
    }

    private void calculateTotals() {
        if(individualTotal.get(manditory[0])!=null)
            commonCharges += individualTotal.get(manditory[0])[headers.length-1];
        Double[] chargesPerPerson;
        calculatedTotal[0] = commonCharges;
        commonCharges = Math.round(commonCharges) / 10.0;
        for(String key : individualTotal.keySet()){
            chargesPerPerson = individualTotal.get(key);
            if(key.matches("^[^A-Za-z]+.*")) {
                if(chargesPerPerson[0]>0) {
                    chargesPerPerson[4] -= chargesPerPerson[0];
                    chargesPerPerson[0] = commonCharges;
                    chargesPerPerson[4] += commonCharges;
                }
                for(int i =1;i<headers.length;i++)
                    calculatedTotal[i]+= chargesPerPerson[i];
            }
        }
        individualTotal.put("CalculatedTotals", calculatedTotal);
    }

    private void extractData(String group, String charges) {
        charges = charges.replace("-","$0").replace("$","").trim();
        charges = charges.replaceAll("[\\s]+"," ");
        individualTotal.putIfAbsent(group, new Double[headers.length]);
        Double[] chargesPerType = individualTotal.get(group);
        String[]  chargesArray = charges.split(" ");
        for(int i =0;i<headers.length;i++) {
            if(chargesPerType[i]==null) chargesPerType[i] = new Double(0);
            chargesPerType[i] += Double.parseDouble(chargesArray[i]);
        }
        if(group.matches("^[^A-Za-z]+.*") && chargesPerType[0]>0) {
            commonCharges += chargesPerType[0];
            totalLines++;
        }

    }


    private List<String> formatData(String[] text){
        List<String> formattedData = new ArrayList<>();
        boolean considerThisData = false;
        for(String line : text){
            if(line.equalsIgnoreCase("THIS BILL SUMMARY")) {
                considerThisData = true;
                continue;
            }
            if(!considerThisData ) continue;
            if(line.equalsIgnoreCase("DETAILED CHARGES")) break;
            if(!line.startsWith(manditory[0]) && !line.startsWith(manditory[1])) {
                line = line.replaceAll("([\\s]*-?[a-zA-Z]*[\\s]*[a-zA-Z]+)","");
            }
            if(line.length()>0)
                formattedData.add(line.replaceAll("\\u00a0"," "));
        }
        return formattedData;
    }
}
