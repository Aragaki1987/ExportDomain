package com.san.service;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by An Nguyen on 4/15/2017.
 */
public class ExportService implements Job {
    private Properties filterProps;
    private Properties properties;
    private String sourceFilter;
    private String resultLocation;

    public ExportService() {

    }


    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        System.out.println("Loading properties");
        try {
            this.sourceFilter = jobExecutionContext.getScheduler().getContext().getString("filterFile");
            System.out.println("Filter file : " + this.sourceFilter);
            String resultLocation = jobExecutionContext.getScheduler().getContext().getString("resultLocation");
            System.out.println("Result Location : " + resultLocation);
            properties = (Properties) jobExecutionContext.getScheduler().getContext().get("properties");

            System.out.println("Load Filter");
            loadFilter(this.sourceFilter);

            System.out.println("Start cron job....");
            run();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(true);

        boolean sleep = false;
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        int hour = utc.getHour();
        int min = utc.getMinute();
        boolean isLogin = false;

        while (!sleep) {
            System.out.println("Hour " + hour);
            if (!validateTime(hour, min)) {
                System.out.println("CURRENT TIME IS OUT OF RANGE, WAIT FOR NEXT TURN");
                sleep = true;
            } else {
                try {
                    if(!isLogin) {
                        System.out.println("LOGIN....");
                        isLogin = login(webClient);
                    }

                    /*APPLY Filter to check the number of result*/
                    boolean downloadAble = checkDomainNumber(webClient);
                    List<String> domainList = Collections.emptyList();

                   /* if(downloadAble) {
                        System.out.println("APPLY FILTER....");
                        String url = createFilter();
                        System.out.println("EXPORT DATA....");
                        domainList = exportDataFromPageResultByLink(webClient, url);
                    }
                    System.out.println("THERE ARE " + domainList.size() + " DOMAINS");
                    if (domainList.size() > 0) {
                        System.out.println("START STORE DOMAIN LIST IN THE FILE");
                        storeDomainList(domainList);
                        System.out.println("FINISHED - SLEEP until next run");
                        sleep = true;
                    } else {
                        *//*If there is no new domain, sleep for 1 minute and repeat*//*
                        System.out.println("SLEEP FOR 1 MINUTE...");
                        sleep(60 * 1000);
                    }*/
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        webClient.close();
    }

    private String createFilter() {
        StringBuilder builder = new StringBuilder();
        builder.append("https://member.expireddomains.net/export/expiredcom/?export=textfile&flast12=1");
        if (!filterProps.getProperty("minExBackLink").isEmpty()) {
            System.out.println("minExBackLink : " + filterProps.getProperty("minExBackLink"));
            builder.append("&fmseoextbl=" + filterProps.getProperty("minExBackLink"));
        }
        if (!filterProps.getProperty("maxExBackLink").isEmpty()) {
            System.out.println("maxExBackLink : " + filterProps.getProperty("maxExBackLink"));
            builder.append("&fmseoextblmax=" + filterProps.getProperty("maxExBackLink"));
        }
        if (!filterProps.getProperty("minMajesticRefDomain").isEmpty()) {
            System.out.println("minMajesticRefDomain : " + filterProps.getProperty("minMajesticRefDomain"));
            builder.append("&fmseorefdomains=" + filterProps.getProperty("minMajesticRefDomain"));
        }
        if (!filterProps.getProperty("minMajesticRefIP").isEmpty()) {
            System.out.println("minMajesticRefIP : " + filterProps.getProperty("minMajesticRefIP"));
            builder.append("&fmseorefips=" + filterProps.getProperty("minMajesticRefIP"));
        }
        if (!filterProps.getProperty("minMajesticClassC").isEmpty()) {
            System.out.println("minMajesticClassC : " + filterProps.getProperty("minMajesticClassC"));
            builder.append("&fmseorefsubnets=" + filterProps.getProperty("minMajesticClassC"));
        }
        if (!filterProps.getProperty("minMajesticCitationFlow").isEmpty()) {
            System.out.println("minMajesticCitationFlow : " + filterProps.getProperty("minMajesticCitationFlow"));
            builder.append("&fmseocf=" + filterProps.getProperty("minMajesticCitationFlow"));
        }
        if (!filterProps.getProperty("minMajesticTrustFlow").isEmpty()) {
            System.out.println("minMajesticTrustFlow : " + filterProps.getProperty("minMajesticTrustFlow"));
            builder.append("&fmseotf=" + filterProps.getProperty("minMajesticTrustFlow"));
        }
        if (!filterProps.getProperty("minMajesticTrustRatio").isEmpty()) {
            System.out.println("minMajesticTrustRatio : " + filterProps.getProperty("minMajesticTrustRatio"));
            builder.append("&fmseotr=" + filterProps.getProperty("minMajesticTrustRatio"));
        }

        return builder.toString();
    }

    //https://member.expireddomains.net/export/expiredcom201704/?export=textfile&flast12=1&flimit=200&fmseocf=10&fmseotf=10&fmseoextbl=10&fmseoextblmax=10000&fmseorefdomains=10&fmseorefips=10&fmseorefsubnets=10&fmseotr=10
    private List<String> exportDataFromPageResultByLink(WebClient webClient, String url) {
        List<String> result = new ArrayList<String>();
        try {
            System.out.println("Download file from URL : " + url);
            Page page = webClient.getPage(url);
            WebResponse response = page.getWebResponse();

            InputStream contentAsStream = response.getContentAsStream();

            result = getStringFromInputStream(contentAsStream);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // convert InputStream to String list
    private List<String> getStringFromInputStream(InputStream is) {
        List<String> domainList = new ArrayList<String>();
        BufferedReader br = null;

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                if (line.length() > 2) { //make sure it's a domain
                    domainList.add(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return domainList;

    }


    private void loadFilter(String sourceFilter) {
        filterProps = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(sourceFilter);
            filterProps.load(is);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR : Filter config file is not found");
        } catch (IOException e) {
            System.out.println("ERROR : Cannot load checkDomainNumber from file");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public boolean login(final WebClient webClient) throws IOException {

        CookieManager cookieMan = webClient.getCookieManager();
        cookieMan.setCookiesEnabled(true);

        HtmlPage page = webClient.getPage("https://www.expireddomains.net/login/");
        HtmlForm form = page.getForms().get(1);

        HtmlButton button = (HtmlButton) form.getFirstElementChild()
                .getNextElementSibling()
                .getNextElementSibling()
                .getNextElementSibling()
                .getFirstElementChild()
                .getFirstElementChild();

        HtmlTextInput login = (HtmlTextInput) page.getElementById("inputLogin");
        HtmlPasswordInput password = (HtmlPasswordInput) page.getElementById("inputPassword");

        login.setText(properties.getProperty("username"));
        password.setText(properties.getProperty("password"));

        button.click();

        return true;
    }

    public boolean checkDomainNumber(WebClient webClient) throws IOException, InterruptedException {

        HtmlPage page = webClient.getPage("https://member.expireddomains.net/domains/expiredcom/");
        HtmlForm filterForm = page.getForms().get(1);

        HtmlCheckBoxInput last12Hour = (HtmlCheckBoxInput) page.getElementById("flast12");
        last12Hour.setChecked(true);

        HtmlNumberInput minExBackLink = (HtmlNumberInput) page.getElementById("fmseoextbl");
        minExBackLink.setValueAttribute(filterProps.getProperty("minExBackLink"));

        HtmlNumberInput maxExBackLink = (HtmlNumberInput) page.getElementById("fmseoextblmax");
        maxExBackLink.setValueAttribute(filterProps.getProperty("maxExBackLink"));

        HtmlNumberInput minMajesticRefDomain = (HtmlNumberInput) page.getElementById("fmseorefdomains");
        minMajesticRefDomain.setValueAttribute(filterProps.getProperty("minMajesticRefDomain"));

        HtmlNumberInput minMajesticRefIP = (HtmlNumberInput) page.getElementById("fmseorefips");
        minMajesticRefIP.setValueAttribute(filterProps.getProperty("minMajesticRefIP"));

        HtmlNumberInput minMajesticClassC = (HtmlNumberInput) page.getElementById("fmseorefsubnets");
        minMajesticClassC.setValueAttribute(filterProps.getProperty("minMajesticClassC"));

        HtmlNumberInput minMajesticCitationFlow = (HtmlNumberInput) page.getElementById("fmseocf");
        minMajesticCitationFlow.setValueAttribute(filterProps.getProperty("minMajesticCitationFlow"));

        HtmlNumberInput minMajesticTrustFlow = (HtmlNumberInput) page.getElementById("fmseotf");
        minMajesticTrustFlow.setValueAttribute(filterProps.getProperty("minMajesticTrustFlow"));

        HtmlNumberInput minMajesticTrustRatio = (HtmlNumberInput) page.getElementById("fmseotr");
        minMajesticTrustRatio.setValueAttribute(filterProps.getProperty("minMajesticTrustRatio"));

        /*submit*/

        HtmlInput button = filterForm.getInputByValue("Apply Filter");

        webClient.waitForBackgroundJavaScriptStartingBefore(3000);
        HtmlPage pageWithFilter = button.click();
        synchronized (pageWithFilter) {
            pageWithFilter.wait(10000);
        }

        HtmlNumberInput test = (HtmlNumberInput) pageWithFilter.getElementById("fmseotr");
        System.out.println(test);

        String stringSelect = pageWithFilter.getElementById("content")
                .getFirstElementChild()
                .getFirstElementChild()
                .getNextElementSibling()
                .getFirstElementChild()
                .getNextElementSibling()
                .getFirstElementChild().getTextContent();
        System.out.println(stringSelect);

        HtmlTable tableContent = (HtmlTable) pageWithFilter.getElementById("content")
                .getFirstElementChild()
                .getFirstElementChild()
                .getNextElementSibling()
                .getFirstElementChild()
                .getNextElementSibling()
                .getNextElementSibling();
        if (tableContent != null) {
            HtmlTableBody body = tableContent.getBodies().get(0);

            int rowNum = body.getRows().size();
            System.out.println("Row num is " + rowNum);
            if (rowNum > 1) {
                return true;
            } else if(rowNum <=0 ) {
                return false;
            } else {
                HtmlTableRow row = body.getRows().get(0);
                HtmlTableCell cell = row.getCell(1);
                System.out.println(cell.getTextContent());
            }

        }

        return false;
    }

    private boolean validateTime(int hour, int min) {
        int startHour;
        int startMin;
        int endHour;
        int endMin;
        try {
            startHour = Integer.valueOf(properties.getProperty("start.hour"));
            startMin = Integer.valueOf(properties.getProperty("start.minute"));
            endHour = Integer.valueOf(properties.getProperty("end.hour"));
            endMin = Integer.valueOf(properties.getProperty("end.minute"));
        } catch (Exception ex) {
            System.out.println("ERROR when parsing start time and end time...");
            return false;
        }

        return hour >= startHour && hour <= endHour;
    }

    private void sleep(int number) {
        try {
            Thread.sleep(number);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void storeDomainList(List<String> domainList) {
        Calendar now = Calendar.getInstance();
        String resultFile = resultLocation + "/deleted-com-domains-" + now.get(Calendar.DATE)
                + "_" + now.get(Calendar.MONTH)
                + "_" + now.get(Calendar.YEAR)
                + ".txt";
        System.out.println("FileName : " + resultFile);

        File file = new File(resultFile);
        PrintWriter out = null;
        try {
            if (file.exists()) {
                System.out.println("ERROR: File exist.");
            } else {
                file.createNewFile();
                out = new PrintWriter(new File(resultFile));
                int index = 0;
                out.println("=====================================0=====================================");
                for (String line : domainList) {
                    out.println("www." + line);
                    index++;
                    if (index % 400 == 0) {
                        out.println("=====================================" + index + "=====================================");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    public Properties getFilterProps() {
        return filterProps;
    }

    public void setFilterProps(Properties filterProps) {
        this.filterProps = filterProps;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getSourceFilter() {
        return sourceFilter;
    }

    public void setSourceFilter(String sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    public String getResultLocation() {
        return resultLocation;
    }

    public void setResultLocation(String resultLocation) {
        this.resultLocation = resultLocation;
    }
}
