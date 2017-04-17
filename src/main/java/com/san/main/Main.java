package com.san.main;

import com.san.service.ExportService;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by nguye on 4/15/2017.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length != 2) {
            System.out.println("Application need 2 parameters to execute. Filter source file location and result file location. ");
        }
        String filterFile = args[0];
        String resultLocation = args[1];
        Properties properties = loadProperties();


        JobDetail job = JobBuilder.newJob(ExportService.class).withIdentity("exportJob", "groupExport").build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("exportJob", "groupExport")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule("0/5 * * * * ?"))
                .build();


        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.getContext().put("filterFile", filterFile);
        scheduler.getContext().put("resultLocation", resultLocation);
        scheduler.getContext().put("properties", properties);
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
    }

    public static Properties loadProperties() {
        Properties properties = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream("./export.properties");
            properties.load(is);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR : Properties config file is not found");
        } catch (IOException e) {
            System.out.println("ERROR : Cannot load properties from file");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return properties;
    }
}
