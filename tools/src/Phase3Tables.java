// GENERATED from fd2.xml (Fix11) — do not hand-edit.
// LOOKUP_CP[i] = code point of long_press_data token[3] for key LETTERS[i].
// Token[3] is unique across all 26 keys (verified). COL: 0=center keys, 1=left edge (q,a,z), 2=right edge (p,l,m).
public class Phase3Tables {
    public static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    public static final int[] LOOKUP_CP = {
        0x1F496, 0x1F382, 0x1F431, 0x1F436, 0x1F929, 0x1F355, 0x1F602, 0x1F60A, 0x1F60E, 0x1F92A, 0x1F48B, 0x1F49D, 0x1F3B8, 0x1F319, 0x1F62E, 0x1F942, 0x1F622, 0x1F621, 0x1F61E, 0x1F914, 0x1F308, 0x1F3C6, 0x1F640, 0x1F576, 0x1F64C, 0x1F62A };
    public static final int[] LOOKUP_COL = {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
    // slot indices (data-index in the 9-item list) per column class
    // center keys: down=0 center=3 right=4 left=5 up=6
    // edge keys (left+right): down=1 center=4 right/left=3/5 (see below) up=7
    public static final int[][] SLOT_IDX = { {0,3,4,5,6}, {1,4,5,3,7}, {1,4,3,5,7} };
    // SLOT_IDX[col] = {down, center, right, left, up} data indices
}
