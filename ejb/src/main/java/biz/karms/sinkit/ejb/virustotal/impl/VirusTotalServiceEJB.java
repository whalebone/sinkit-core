package biz.karms.sinkit.ejb.virustotal.impl;

import biz.karms.sinkit.ejb.virustotal.VirusTotalService;
import com.kanishka.virustotal.dto.FileScanReport;
import com.kanishka.virustotal.dto.ScanInfo;
import com.kanishka.virustotal.exception.InvalidArguentsException;
import com.kanishka.virustotal.exception.QuotaExceededException;
import com.kanishka.virustotal.exception.UnauthorizedAccessException;
import com.kanishka.virustotalv2.VirustotalPublicV2;

import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by tkozel on 31.7.15.
 */
@Stateless
public class VirusTotalServiceEJB implements VirusTotalService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Inject
    private Logger log;

    @Inject
    private VirustotalPublicV2 virusTotalClient;

    @Override
    public ScanInfo scanUrl(String url) throws QuotaExceededException, InvalidArguentsException, UnauthorizedAccessException, IOException {
        log.finest("VT scanning URL: " + url);
        ScanInfo[] scanInfoArr = virusTotalClient.scanUrls(new String[]{url});
//        for (ScanInfo scanInformation : scanInfoArr) {
//            log.info("___SCAN INFORMATION___");
//            log.info("MD5 :\t" + scanInformation.getMd5());
//            log.info("Perma Link :\t" + scanInformation.getPermalink());
//            log.info("Resource :\t" + scanInformation.getResource());
//            log.info("Scan Date :\t" + scanInformation.getScanDate());
//            log.info("Scan Id :\t" + scanInformation.getScanId());
//            log.info("SHA1 :\t" + scanInformation.getSha1());
//            log.info("SHA256 :\t" + scanInformation.getSha256());
//            log.info("Verbose Msg :\t" + scanInformation.getVerboseMessage());
//            log.info("Response Code :\t" + scanInformation.getResponseCode());
//            log.info("done.");
//        }
        return scanInfoArr[0];
    }

    @Override
    public FileScanReport getUrlScanReport(String url) throws QuotaExceededException, InvalidArguentsException, UnauthorizedAccessException, IOException {
        log.finest("VT getting report of URL scan for UR: " + url);
        FileScanReport[] reports = virusTotalClient.getUrlScanReport(new String[]{url}, false);
        return reports[0];
    }

    @Override
    public Date parseDate(String date) throws ParseException {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        return df.parse(date);
    }
}
