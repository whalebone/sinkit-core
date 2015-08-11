package biz.karms.sinkit.eventlog;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tkozel on 3.8.15.
 */
public enum VirusTotalRequestStatus {

    // initial state of Virus Total request, it's waiting for processing
    @SerializedName("waiting")
    WAITING,

    // Virus Total has already been asked for scan of given URL and the request is now waiting for report
    @SerializedName("waiting_for_report")
    WAITING_FOR_REPORT,

    // the request is finished, VirusTotal report has been received and corresponding IoCs were enriched
    // this is end state
    @SerializedName("finished")
    FINISHED,

    // there is already actual report from Virus Total in Core and it's not necessary to ask Virus Total
    // this is end state
    @SerializedName("not_needed")
    NOT_NEEDED
}
