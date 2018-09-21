package biz.karms.sinkit.ejb.protostream;

/**
 * @author Michal Karm Babacek
 *         BLACK - Block, malware, undesirable
 *         WHITE - Unconditionally let pass through
 *         LOG   - Process for auditing purposes, but take no further action
 *         CHECK - No prior knowledge about any of the aforementioned statuses, so call backend to check
 */
public enum Action {
    BLACK,
    WHITE,
    LOG,
    CHECK
}
