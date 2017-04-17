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
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

/**
 * Created by nguye on 4/15/2017.
 */
public class ExportService {
    Properties filterProps;

    public ExportService(String sourceFilter, String resultFile) {
        loadFilter(sourceFilter);
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
        WebClient webClient = new WebClient(BrowserVersion.EDGE);
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

        login.setText("aragaki");
        password.setText("meokeugaugau");

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
        //return webClient;
    }

    public void exportDataFromPageResult(HtmlPage page) throws IOException {
        HtmlAnchor downloadLink = page.getAnchorByHref("/export/expiredcom/?export=textfile");
        System.out.println(downloadLink);

        String is = downloadLink.click().getWebResponse().getContentAsString();

        System.out.println(is);
    }


}
