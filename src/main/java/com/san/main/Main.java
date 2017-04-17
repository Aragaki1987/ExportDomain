package com.san.main;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.san.service.ExportService;

import java.util.Map;

/**
 * Created by nguye on 4/15/2017.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        /*if(args.length != 2) {
            System.out.println("Application need 2 parameters to execute. Filter source file location and result file location. ");
        }
        String filterFile = args[0];
        String resultFile = args[1];*/
        ExportService exportService = new ExportService("F:\\Upwork\\epxort_domain\\export-domain\\src\\main\\resources\\ExpiredDomainConfig.txt", null);
        WebClient webClient = exportService.login();
        HtmlPage page = exportService.filter(webClient);
        exportService.exportDataFromPageResult(page);
    }
}