package biz.karms.sinkit.ejb.virustotal;

import com.kanishka.virustotal.dto.FileScanReport;
import com.kanishka.virustotal.dto.ScanInfo;
import com.kanishka.virustotal.exception.InvalidArguentsException;
import com.kanishka.virustotal.exception.QuotaExceededException;
import com.kanishka.virustotal.exception.UnauthorizedAccessException;

import javax.ejb.Local;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface VirusTotalService {
    ScanInfo scanUrl(String url) throws QuotaExceededException, InvalidArguentsException, UnauthorizedAccessException, IOException;

    FileScanReport getUrlScanReport(String url) throws QuotaExceededException, InvalidArguentsException, UnauthorizedAccessException, IOException;

    Date parseDate(String date) throws ParseException;
}
