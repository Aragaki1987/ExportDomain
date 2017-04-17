package com.san.service;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlNumberInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by nguye on 4/15/2017.
 */
public class ExportService implements Job {
    private Properties filterProps;
    private Properties properties;
    private String sourceFilter;
    private String resultFile;

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

            Calendar now = Calendar.getInstance();
            this.resultFile = resultLocation + "/deleted-com-domains-" + now.get(Calendar.DATE)
                    + "_" + now.get(Calendar.MONTH)
                    + "_" + now.get(Calendar.YEAR)
                    + ".txt";
            System.out.println("FileName : " + this.resultFile);
            System.out.println("Load Filter");
            loadFilter(this.sourceFilter);

            System.out.println("Start cron job....");
            run();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        WebClient webClient;
        boolean sleep = false;
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        int hour = utc.getHour();
        int min = utc.getMinute();

        while (!sleep) {

            if (!validateTime(hour, min)) {
                System.out.println("CURRENT TIME IS OUT OF RANGE, WAIT FOR NEXT TURN");
                sleep = true;
            } else {
                try {
                    System.out.println("LOGIN....");
                    webClient = login();
                    System.out.println("APPLY FILTER....");
                    HtmlPage page = filter(webClient);
                    System.out.println("EXPORT DATA....");
                    Set<String> domainList = exportDataFromPageResult(page);
                    System.out.println("THERE ARE " + domainList.size() + " DOMAINS");
                    if (domainList.size() > 0) {
                        System.out.println("START STORE DOMAIN LIST IN THE FILE");
                        storeDomainList(domainList);
                        System.out.println("FINISHED");
                        sleep = true;
                    } else {
                        /*If there is no new domain, sleep for 1 minute and repeat*/
                        sleep(60 * 1000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
            System.out.println("ERROR : Cannot load filter from file");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public WebClient login() throws IOException {
        WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        CookieManager cookieMan = new CookieManager();
        cookieMan = webClient.getCookieManager();
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

        return webClient;
    }

    public HtmlPage filter(WebClient webClient) throws IOException {
        HtmlPage page = webClient.getPage("https://member.expireddomains.net/domains/expiredcom/");
        HtmlForm filterForm = page.getForms().get(1);

        HtmlCheckBoxInput last12Hour = (HtmlCheckBoxInput) page.getElementById("flast12");
        last12Hour.setChecked(true);

        HtmlNumberInput minExBackLink = (HtmlNumberInput) page.getElementById("fmseoextbl");
        minExBackLink.setText(filterProps.getProperty("minExBackLink"));

        HtmlNumberInput maxExBackLink = (HtmlNumberInput) page.getElementById("fmseoextblmax");
        maxExBackLink.setText(filterProps.getProperty("maxExBackLink"));

        HtmlNumberInput minMajesticRefDomain = (HtmlNumberInput) page.getElementById("fmseorefdomains");
        minMajesticRefDomain.setText(filterProps.getProperty("minMajesticRefDomain"));

        HtmlNumberInput minMajesticRefIP = (HtmlNumberInput) page.getElementById("fmseorefips");
        minMajesticRefIP.setText(filterProps.getProperty("minMajesticRefIP"));

        HtmlNumberInput minMajesticClassC = (HtmlNumberInput) page.getElementById("fmseorefsubnets");
        minMajesticClassC.setText(filterProps.getProperty("minMajesticClassC"));

        HtmlNumberInput minMajesticCitationFlow = (HtmlNumberInput) page.getElementById("fmseocf");
        minMajesticCitationFlow.setText(filterProps.getProperty("minMajesticCitationFlow"));

        HtmlNumberInput minMajesticTrustFlow = (HtmlNumberInput) page.getElementById("fmseotf");
        minMajesticTrustFlow.setText(filterProps.getProperty("minMajesticTrustFlow"));

        HtmlNumberInput minMajesticTrustRatio = (HtmlNumberInput) page.getElementById("fmseotr");
        minMajesticTrustRatio.setText(filterProps.getProperty("minMajesticTrustRatio"));

        /*submit*/

        HtmlInput button = filterForm.getInputByValue("Apply Filter");

        return button.click();
    }

    public Set<String> exportDataFromPageResult(HtmlPage page) throws IOException {
        HtmlAnchor downloadLink = page.getAnchorByHref("/export/expiredcom/?export=textfile");
        System.out.println(downloadLink);

        String is = downloadLink.click().getWebResponse().getContentAsString();

        String[] result = is.split("\n");

        return new HashSet<String>(Arrays.asList(result));
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

        System.out.println("hour : " + hour);
        System.out.println("min : " + min);
        System.out.println("startHour : " + startHour);
        System.out.println("startMin : " + startMin);
        System.out.println("endHour : " + endHour);
        System.out.println("endMin : " + endMin);

        return hour >= startHour && hour <= endHour && min >= startMin && min <= endMin;
    }

    private void sleep(int number) {
        try {
            Thread.sleep(number);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void storeDomainList(Set<String> domainList) {
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
                    out.print(line);
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

    public String getResultFile() {
        return resultFile;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
    }
}
